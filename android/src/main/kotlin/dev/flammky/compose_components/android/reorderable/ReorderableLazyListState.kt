package dev.flammky.compose_components.android.reorderable

import android.util.Log
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import dev.flammky.compose_components.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class ReorderableLazyListState internal constructor(
    val coroutineScope: CoroutineScope,
    val lazyListState: LazyListState,
    private val onDragStart: ((item: ItemPosition) -> Boolean)?,
    // should provide `cancelled` reason
    private val onDragEnd: ((cancelled: Boolean, from: ItemPosition, to: ItemPosition) -> Unit)?,
    // should we handle the listing ?
    private val movable: ((from: ItemPosition, to: ItemPosition) -> Boolean)?,
    private val onMove: ((from: ItemPosition, to: ItemPosition) -> Boolean),
) : ReorderableScrollableState<LazyListItemInfo>() {

    private val _applier = RealReorderableLazyListApplier(this)

    // TODO: we can wrap these into an independent instance
    private var _draggingId: Long? = null
    private var _draggingItemStartSnapshot by mutableStateOf<LazyListItemInfo?>(null)
    private var _draggingItemLatestSnapshot by mutableStateOf<LazyListItemInfo?>(null)
    private var _expectDraggingItemCurrentIndex by mutableStateOf<Int?>(null)
    private var _draggingItemStartOffset = Offset.Zero
    private var _draggingItemDeltaFromStart by mutableStateOf(Offset.Zero)
    private var _draggingItemDeltaFromCurrent by mutableStateOf(Offset.Zero)
    private var _draggingDropTarget: LazyListItemInfo? = null

    private var _autoScrollerJob: Job? = null

    internal val applier: ReorderableLazyListApplier = _applier
    internal val childReorderStartChannel: Channel<ReorderDragStart> = Channel()
    internal override val scrollChannel: Channel<Float> = Channel()

    override val isVerticalScroll: Boolean =
        lazyListState.layoutInfo.orientation == Orientation.Vertical

    override val isHorizontalScroll: Boolean =
        lazyListState.layoutInfo.orientation == Orientation.Horizontal

    override val visibleItemsInfo: List<LazyListItemInfo>
        @SnapshotRead
        get() = lazyListState.layoutInfo.visibleItemsInfo

    override val reverseLayout: Boolean =
        lazyListState.layoutInfo.reverseLayout

    override val expectDraggingItemIndex: Int?
        @SnapshotRead
        get() = _expectDraggingItemCurrentIndex

    override val currentLayoutDraggingItemIndex: Int?
        @SnapshotRead
        get() = _draggingItemStartSnapshot?.let {
            _applier.indexOfKey(it.key).takeIf { i -> i != -1 }
        }

    override val draggingItemKey: Any?
        @SnapshotRead
        get() = _draggingItemStartSnapshot?.itemKey

    override val draggingItemPosition: ItemPosition?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let {
                ItemPosition(expectDraggingItemIndex ?: return@let null, it.itemKey)
            }

    override val draggingItemDelta: Offset
        @SnapshotRead
        get() = _draggingItemStartSnapshot?.let { snap ->
            visibleItemsInfo.fastFirstOrNull { visible ->
                visible.key == snap.key
            }?.let { inLayout ->
                if (isVerticalScroll) {
                    verticalOffset(snap.offset + _draggingItemDeltaFromStart.y - inLayout.offset)
                } else if (isHorizontalScroll) {
                    horizontalOffset(snap.offset + _draggingItemDeltaFromStart.x - inLayout.offset)
                } else exhaustedStateException()
            }
        } ?: Offset.Zero

    override val draggingItemLeftPos: Float?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { leftPos + _draggingItemDeltaFromStart.x }
            }

    override val draggingItemTopPos: Float?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { topPos + _draggingItemDeltaFromStart.y }
            }

    override val draggingItemRightPos: Float?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { rightPos + _draggingItemDeltaFromStart.x }
            }

    override val draggingItemBottomPos: Float?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { bottomPos + _draggingItemDeltaFromStart.y }
            }

    override val draggingItemLayoutPosition: LayoutPosition?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run {
                        val delta = _draggingItemDeltaFromStart
                        LayoutPosition(
                            leftPos + delta.x,
                            topPos + delta.y,
                            rightPos + delta.x,
                            bottomPos + delta.y,
                        )
                    }
            }

    override val draggingItemLayoutInfo: LazyListItemInfo?
        @SnapshotRead
        get() = _draggingItemStartSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {  visibleItem ->
                        visibleItem.key == draggingItem.key
                    }
            }

    override val LazyListItemInfo.layoutPositionInParent: LayoutPosition
        get() = LayoutPosition(
            leftPos.toFloat(),
            topPos.toFloat(),
            rightPos.toFloat(),
            bottomPos.toFloat(),
        )

    override val LazyListItemInfo.linearLayoutPositionInParent: LinearLayoutPosition
        get() = LinearLayoutPosition(
            startPos.toFloat(),
            endPos.toFloat()
        )

    override val draggingItemStartPos: Float?
        get() = if (isVerticalScroll) draggingItemTopPos else draggingItemLeftPos

    override val draggingItemEndPos: Float?
        get() = if (isVerticalScroll) draggingItemBottomPos else draggingItemRightPos

    override val firstVisibleItemIndex: Int
        @SnapshotRead
        get() = lazyListState.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int
        @SnapshotRead
        get() = lazyListState.firstVisibleItemScrollOffset

    override val viewportStartOffset: Int
        @SnapshotRead
        get() = lazyListState.layoutInfo.viewportStartOffset

    override val viewportEndOffset: Int
        @SnapshotRead
        get() = lazyListState.layoutInfo.viewportEndOffset

    override val LazyListItemInfo.itemIndex: Int
        get() = index

    override val LazyListItemInfo.itemKey: Any
        get() = key

    override val LazyListItemInfo.leftPos: Int
        @SnapshotRead
        get() = when {
            isVerticalScroll -> 0
            lazyListState.layoutInfo.reverseLayout -> {
                lazyListState.layoutInfo.viewportSize.width - offset - size
            }
            else -> offset
        }

    override val LazyListItemInfo.rightPos: Int
        @SnapshotRead
        get() = when {
            isVerticalScroll -> 0
            lazyListState.layoutInfo.reverseLayout -> {
                lazyListState.layoutInfo.viewportSize.width - offset
            }
            else -> offset + size
        }

    override val LazyListItemInfo.topPos: Int
        @SnapshotRead
        get() = when {
            !isVerticalScroll -> 0
            lazyListState.layoutInfo.reverseLayout -> {
                lazyListState.layoutInfo.viewportSize.height - offset - size
            }
            else -> offset
        }

    override val LazyListItemInfo.bottomPos: Int
        @SnapshotRead
        get() = when {
            !isVerticalScroll -> 0
            lazyListState.layoutInfo.reverseLayout -> {
                lazyListState.layoutInfo.viewportSize.height - offset
            }
            else -> offset + size
        }

    override val LazyListItemInfo.startPos: Int
        get() = if (isVerticalScroll) {
            if (lazyListState.layoutInfo.reverseLayout)
                lazyListState.layoutInfo.viewportSize.height - offset - size
            else
                offset
        } else {
            if (lazyListState.layoutInfo.reverseLayout)
                lazyListState.layoutInfo.viewportSize.width - offset - size
            else
                offset
        }

    override val LazyListItemInfo.endPos: Int
        get() = if (isVerticalScroll) {
            if (lazyListState.layoutInfo.reverseLayout)
                lazyListState.layoutInfo.viewportSize.height - offset
            else
                offset + size
        } else {
            if (lazyListState.layoutInfo.reverseLayout)
                lazyListState.layoutInfo.viewportSize.width - offset
            else
                offset + size
        }

    override val LazyListItemInfo.height: Int
        get() = if (isVerticalScroll) {
            size
        } else {
            0
        }

    override val LazyListItemInfo.width: Int
        get() = if (isVerticalScroll) {
            size
        } else {
            0
        }

    /**
     * @see ReorderableState.onStartDrag
     */
    internal override fun onStartDrag(
        id: Long,
        startX: Int,
        startY: Int,
        expectKey: Any,
        expectIndex: Int
    ): Boolean {
        Log.d(
            "Reorderable_DEBUG",
            "onStartDrag(id=$id, startX=$startX, startY=$startY, expectKey=$expectKey, expectIndex=$expectIndex)"
        )
        internalReorderableStateCheck(_draggingId == null) {
            "Unexpected Dragging ID during onStartDrag, " +
                    "expect=${null}, actual=$_draggingId, inMainLooper=${inMainLooper()}"
        }
        val x: Int
        val y: Int
        // consider the viewport offset (Content Padding)
        if (isVerticalScroll) {
            x = 0
            y = viewportStartOffset + startY
        } else {
            x = viewportStartOffset + startX
            y = 0
        }
        // find the dragged Item according to the Drag input position
        return visibleItemsInfo
            .fastFirstOrNull {
                x in it.leftPos..it.rightPos && y in it.topPos..it.bottomPos
            }
            ?.takeIf { itemInfo ->
                Log.d(
                    "Reorderable_DEBUG",
                    "onStartDrag(id=$id, startX=$startX, startY=$startY, expectKey=$expectKey, expectIndex=$expectIndex) takeIf ${itemInfo.index} ${itemInfo.key}"
                )
                itemInfo.key == expectKey &&
                itemInfo.index == expectIndex &&
                onDragStart?.invoke(ItemPosition(itemInfo.itemIndex, itemInfo.itemKey)) != false
            }
            ?.let { itemInfo ->
                _draggingId = id
                _draggingItemStartSnapshot = itemInfo
                _expectDraggingItemCurrentIndex = itemInfo.index
                _draggingItemStartOffset = Offset(x.toFloat(), y.toFloat())
            } != null
    }

    internal override fun onDrag(
        id: Long,
        dragX: Int,
        dragY: Int,
        expectKey: Any
    ): Boolean {
        Log.d(
            "Reorderable_DEBUG",
            "onDrag(id=$id, drag=$dragX, dragY=$dragY, expectKey=$expectKey)"
        )
        internalReorderableStateCheck(id == _draggingId) {
            "Unexpected Dragging ID during onDrag, " +
                "expect=$id, actual=$_draggingId, inMainLooper=${inMainLooper()}"
        }
        val snap = _draggingItemStartSnapshot
            ?: return false
        internalReorderableStateCheck(expectKey == snap.key) {
            "Unexpected Item Key during onDrag, " +
                    "expect=${expectKey}, actual=${snap.key}, inMainLooper=${inMainLooper()}"
        }
        val draggingInfo = lazyListState.layoutInfo.visibleItemsInfo
            .fastFirstOrNull {  visibleItem ->
                visibleItem.key == snap.key
            }
            ?: return false
        val dragDelta =
            if (isVerticalScroll) {
                verticalOffset(_draggingItemDeltaFromStart.y + dragY)
            } else {
                horizontalOffset(_draggingItemDeltaFromStart.x + dragX)
            }.also {
                _draggingItemDeltaFromStart = it
            }
        val checkMoveAllow = checkShouldMoveToTarget(
            deltaX = dragDelta.x.toInt(),
            deltaY = dragDelta.y.toInt(),
            snap = snap,
            draggingInfo = draggingInfo
        )
        if (!checkMoveAllow) {
            return false
        }
        val scrollAllow = checkOnDragOverscroll(
            draggingInfo, dragDelta
        )
        if (!scrollAllow) {
            return false
        }
        return true
    }

    private fun checkOnDragOverscroll(
        draggingItemInfo: LazyListItemInfo,
        draggingDelta: Offset
    ): Boolean {
        autoscroll(calculateOverscrollOffset(draggingItemInfo, draggingDelta))
        return true
    }

    private fun calculateOverscrollOffset(
        draggingItemInfo: LazyListItemInfo,
        draggingDelta: Offset
    ): Float {
        val delta =
            if (isVerticalScroll) {
                draggingDelta.y
            } else {
                draggingDelta.x
            }
        return when {
            delta < 0 -> {
                val startPos =
                    if (isVerticalScroll)
                        draggingItemInfo.topPos + delta
                    else
                        draggingItemInfo.leftPos + delta
                (startPos - viewportStartOffset).coerceAtLeast(0f)
            }
            delta > 0 -> {
                val endPos =
                    if (isVerticalScroll)
                        draggingItemInfo.bottomPos + delta
                    else
                        draggingItemInfo.rightPos + delta
                (endPos - viewportEndOffset).coerceAtLeast(0f)
            }
            else -> 0f
        }
    }

    private fun calculateCurrentAutoScrollOffset(
        frameTimeMillis: Long,
        maxScrollPx: Float,
    ): Float {
        val (size: Int, outSize: Float) = _draggingItemStartSnapshot
            ?.let { itemInfo ->
                itemInfo.endPos - itemInfo.startPos to calculateOverscrollOffset(itemInfo, _draggingItemDeltaFromStart)
            }
            ?: return 0f
        return interpolateAutoScrollOffset(size, outSize, frameTimeMillis, maxScrollPx)
    }

    private fun interpolateAutoScrollOffset(
        viewLength: Int,
        viewOutOfBoundsLength: Float,
        frameTimeMillis: Long,
        maxScrollPx: Float,
    ): Float {
        // TODO
        return 0f
    }

    private fun autoscroll(scrollOffset: Float) {
        if (scrollOffset != 0f) {
            if (_autoScrollerJob?.isActive == true) {
                return
            }
            _autoScrollerJob = coroutineScope.launch {
                var scroll = scrollOffset
                var startMs = 0L
                while (scroll != 0f && _autoScrollerJob?.isActive == true) {
                    withFrameMillis { frameMs ->
                        if (startMs == 0L) {
                            startMs = frameMs
                        } else {
                            scroll = calculateCurrentAutoScrollOffset(
                                frameMs - startMs,
                                20f
                            )
                        }
                    }
                    scrollChannel.send(scroll)
                }
            }
        } else {
            _autoScrollerJob?.cancel()
            _autoScrollerJob = null
        }
    }

    internal override fun onDragEnd(id: Long, endX: Int, endY: Int, expectKey: Any) {
        Log.d(
            "Reorderable_DEBUG",
            "onDragEnd(id=$id, drag=$endX, dragY=$endY, expectKey=$expectKey)"
        )
        dragEnded(false, id, endX, endY, expectKey)
    }

    internal override fun onDragCancelled(id: Long, endX: Int, endY: Int, expectKey: Any) {
        Log.d(
            "Reorderable_DEBUG",
            "onDragCancelled(id=$id, drag=$endX, dragY=$endY, expectKey=$expectKey)"
        )
        dragEnded(true, id, endX, endY, expectKey)
    }

    private fun dragEnded(
        cancelled: Boolean,
        id: Long,
        endX: Int,
        endY: Int,
        expectKey: Any
    ) {
        internalReorderableStateCheck(id == _draggingId) {
            "Inconsistent Dragging ID during onDragCancelled, " +
                    "expect=$id, actual=$_draggingId, inMainLooper=${inMainLooper()}"
        }
        internalReorderableStateCheck(expectKey == _draggingItemStartSnapshot?.key) {
            "Unexpected Expect Key during onDragCancelled, " +
                    "expect=$expectKey, actual=${_draggingItemStartSnapshot?.key}, inMainLooper=${inMainLooper()}"
        }
        val startSnap = _draggingItemStartSnapshot!!
        this.onDragEnd?.invoke(
            cancelled,
            ItemPosition(startSnap.index, startSnap.key),
            ItemPosition(expectDraggingItemIndex!!, startSnap.key)
        )
        _draggingId = null
        _draggingItemStartSnapshot = null
        _expectDraggingItemCurrentIndex = null
        _draggingItemStartOffset = Offset.Zero
        _draggingItemDeltaFromStart = Offset.Zero
        _draggingDropTarget = null
    }

    private fun checkShouldMoveToTarget(
        deltaX: Int,
        deltaY: Int,
        snap: LazyListItemInfo,
        draggingInfo: LazyListItemInfo
    ): Boolean {
        Log.d(
            "Reorderable_DEBUG",
            "checkShouldMoveToTarget(deltaX=$deltaX, deltaY=$deltaY)"
        )
        if (deltaX == 0 && deltaY == 0) {
            return true
        }
        _draggingDropTarget = null
        // target properties
        val draggingLeftPos: Int
        val draggingTopPos: Int
        val draggingRightPos: Int
        val draggingBottomPos: Int
        val draggingStartPos: Int
        val draggingEndPos: Int
        val draggingCenterPos: Int
        if (isVerticalScroll) {
            draggingLeftPos = 0
            draggingTopPos = snap.topPos + deltaY
            draggingRightPos = 0
            draggingBottomPos = snap.bottomPos + deltaY
            draggingStartPos = draggingTopPos
            draggingEndPos = draggingBottomPos
            draggingCenterPos = (draggingStartPos + draggingEndPos) / 2
        } else if (isHorizontalScroll) {
            draggingLeftPos = snap.leftPos + deltaX
            draggingTopPos = 0
            draggingRightPos = snap.rightPos + deltaX
            draggingBottomPos = 0
            draggingStartPos = draggingLeftPos
            draggingEndPos = draggingRightPos
            draggingCenterPos = (draggingStartPos + draggingEndPos) / 2
        } else exhaustedStateException()

        var next: Boolean? = null
        run {
            // we can improve this
            visibleItemsInfo.fastForEach { visibleItem ->
                if (visibleItem.itemIndex == draggingInfo.itemIndex) {
                    return@fastForEach
                }
                val visibleItemPosition = visibleItem.linearLayoutPositionInParent
                if (
                    draggingStartPos > visibleItemPosition.end ||
                    draggingEndPos < visibleItemPosition.start
                ) {
                    return@fastForEach
                }
                if (
                    movable?.invoke(
                        ItemPosition(expectDraggingItemIndex!!, draggingInfo.key),
                        ItemPosition(visibleItem.index, visibleItem.key)
                    ) != false
                ) {
                    _draggingDropTarget = visibleItem
                    next = visibleItem.index > expectDraggingItemIndex!!
                }
                if (visibleItem.itemIndex > expectDraggingItemIndex!!) {
                    return@run
                }
            }
        }
        _draggingDropTarget?.takeIf { target ->
            val targetCenterPos = (target.startPos + target.endPos) / 2
            Log.d(
                "Reorderable_DEBUG",
                "checkShouldMoveToTarget(deltaX=$deltaX, deltaY=$deltaY), dropTarget takeIf $target $next $targetCenterPos $draggingCenterPos"
            )
            if (next!!) draggingCenterPos > targetCenterPos else draggingCenterPos < targetCenterPos
        }?.let { target ->
            val moved = onMove(
                ItemPosition(draggingInfo.index, draggingInfo.key),
                ItemPosition(target.index, target.key)
            )
            if (!moved) return false
            _expectDraggingItemCurrentIndex = target.index
        }
        return true
    }
}

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onMove: ((/*dragId: Int,*/ from: ItemPosition, to: ItemPosition) -> Boolean)
) = rememberReorderableLazyListState(
    lazyListState = lazyListState,
    onDragStart = null,
    onDragEnd = null,
    movable = null,
    onMove = onMove
)

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onDragStart: ((/*dragId: Int,*/ item: ItemPosition) -> Boolean)?,
    onDragEnd: ((/*dragId: Int,*/ cancelled: Boolean, from: ItemPosition, to: ItemPosition) -> Unit)?,
    movable: ((/*dragId: Int,*/ item: ItemPosition, dragging: ItemPosition) -> Boolean)?,
    onMove: ((/*dragId: Int,*/ from: ItemPosition, to: ItemPosition) -> Boolean),
): ReorderableLazyListState {
    val coroutineScope = rememberCoroutineScope()
    return remember(lazyListState) {
        ReorderableLazyListState(
            coroutineScope = coroutineScope,
            lazyListState = lazyListState,
            onDragStart = onDragStart,
            onDragEnd = onDragEnd,
            movable = movable,
            onMove = onMove
        )
    }
}