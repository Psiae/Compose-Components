package dev.flammky.compose_components.android.reorderable

import android.util.Log
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import dev.flammky.compose_components.android.reorderable.leech.ReorderableState
import dev.flammky.compose_components.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File
import java.util.jar.Manifest
import kotlin.math.pow
import kotlin.math.sign

class ReorderableLazyListState internal constructor(
    val coroutineScope: CoroutineScope,
    val lazyListState: LazyListState,
    private val onDragStart: ((item: ItemPosition) -> Boolean)?,
    // should provide `cancelled` reason
    private val onDragEnd: ((cancelled: Boolean, from: ItemPosition, to: ItemPosition) -> Unit)?,
    private val movable: ((from: ItemPosition, to: ItemPosition) -> Boolean)?,
    private val onMove: ((from: ItemPosition, to: ItemPosition) -> Boolean),
) : ReorderableScrollableState<LazyListItemInfo>() {

    private val _applier = RealReorderableLazyListApplier(this)

    // TODO: we can wrap these into an independent instance
    private var _draggingId: Long? = null
    private var _draggingItemSnapshot by mutableStateOf<LazyListItemInfo?>(null)
    private var _expectDraggingItemCurrentIndex by mutableStateOf<Int?>(null)
    private var _draggingItemDelta by mutableStateOf<Offset>(Offset.Zero)
    private var _draggingDropTarget: LazyListItemInfo? = null

    private var _autoScrollerJob: Job? = null

    private val _draggingItemLayoutInfo: LazyListItemInfo?
        get() = visibleItemsInfo.fastFirstOrNull { visibleItem ->
            visibleItem.itemIndex == expectDraggingItemIndex
        }

    internal val applier: ReorderableLazyListApplier = _applier
    internal override val childDragStartChannel: Channel<DragStart> = Channel()
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
        get() = _draggingItemSnapshot?.let {
            _applier.indexOfKey(it.key).takeIf { i -> i != -1 }
        }

    override val draggingItemKey: Any?
        @SnapshotRead
        get() = _draggingItemSnapshot?.itemKey

    override val draggingItemPosition: ItemPosition?
        @SnapshotRead
        get() = _draggingItemSnapshot
            ?.let {
                ItemPosition(expectDraggingItemIndex ?: return@let null, it.itemKey)
            }

    override val draggingItemDelta: Offset
        @SnapshotRead
        get() = _draggingItemDelta

    override val draggingItemLeftPos: Float
        get() = _draggingItemSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { leftPos + _draggingItemDelta.x }
            } ?: 0f

    override val draggingItemTopPos: Float
        get() = _draggingItemSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { topPos + _draggingItemDelta.y }
            } ?: 0f

    override val draggingItemRightPos: Float
        get() = _draggingItemSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { rightPos + _draggingItemDelta.x }
            } ?: 0f

    override val draggingItemBottomPos: Float
        get() = _draggingItemSnapshot
            ?.let { draggingItem ->
                lazyListState.layoutInfo.visibleItemsInfo
                    .fastFirstOrNull {
                        it.key == draggingItem.key
                    }
                    ?.run { bottomPos + _draggingItemDelta.y }
            } ?: 0f

    override val draggingItemStartPos: Float
        get() = if (isVerticalScroll) draggingItemTopPos else draggingItemLeftPos

    override val draggingItemEndPos: Float
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
    internal override fun onStartDrag(id: Long, startX: Int, startY: Int): Boolean {
        internalReorderableStateCheck(_draggingId == null) {
            "Unexpected Dragging ID during onStartDrag, " +
                    "expect=$id, actual=$_draggingId, inMainLooper=${inMainLooper()}"
        }
        val x: Int
        val y: Int
        // consider the viewport offset (Content Padding)
        if (isVerticalScroll) {
            x = startX
            y = viewportStartOffset + startY
        } else {
            x = viewportStartOffset + startX
            y = startY
        }
        // find the dragged Item according to the Drag input position
        return visibleItemsInfo
            .fastFirstOrNull {
                x in it.leftPos..it.rightPos && y in it.topPos..it.bottomPos
            }
            ?.takeIf { itemInfo ->
                onDragStart?.invoke(ItemPosition(itemInfo.itemIndex, itemInfo.itemKey)) != false
            }
            ?.let { itemInfo ->
                _draggingItemSnapshot = itemInfo
                _draggingId = id
            } != null
    }

    internal override fun onDrag(id: Long, dragX: Int, dragY: Int): Boolean {
        internalReorderableStateCheck(id == _draggingId) {
            "Unexpected Dragging ID during onDrag, " +
                    "expect=$id, actual=$_draggingId, inMainLooper=${inMainLooper()}"
        }
        val selected = _draggingItemSnapshot
            ?: return false
        visibleItemsInfo
            .fastFirstOrNull {
                it.itemKey == selected.itemKey
            }
            ?: return false
        val dragDelta =
            if (isVerticalScroll) {
                verticalOffset(_draggingItemDelta.y + dragY)
            } else {
                horizontalOffset(_draggingItemDelta.x + dragX)
            }.also {
                _draggingItemDelta = it
            }
        findDropTarget(
            deltaX = dragDelta.x.toInt(),
            deltaY = dragDelta.y.toInt(),
            selected = selected
        )?.let { target ->
            val moved = onMove(
                ItemPosition(selected.index, selected.key),
                ItemPosition(target.index, target.key)
            )
            if (!moved) return false
        }
        checkOnDragOverscroll(selected, dragDelta)
        return true
    }

    private fun checkOnDragOverscroll(
        draggingItemInfo: LazyListItemInfo,
        draggingDelta: Offset
    ) {
        autoscroll(calculateOverscrollOffset(draggingItemInfo, draggingDelta))
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
        val (size: Int, outSize: Float) = _draggingItemSnapshot
            ?.let { itemInfo ->
                itemInfo.endPos - itemInfo.startPos to calculateOverscrollOffset(itemInfo, _draggingItemDelta)
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

    internal override fun onDragEnd(id: Long, endX: Int, endY: Int) {
        internalReorderableStateCheck(id == _draggingId) {
            "Unexpected Dragging ID during onDragEnd, expect=$id, actual=$_draggingId, inMainLooper=${inMainLooper()}"
        }
    }

    internal override fun onDragCancelled(id: Long, endX: Int, endY: Int) {
        internalReorderableStateCheck(id == _draggingId) {
            "Inconsistent Dragging ID during onDragCancelled, expect=$id, actual=$_draggingId, inMainLooper=${inMainLooper()}"
        }
    }

    private fun findDropTarget(
        deltaX: Int,
        deltaY: Int,
        selected: LazyListItemInfo
    ): LazyListItemInfo? {
        _draggingDropTarget = null
        // target properties
        val targetLeftPos = selected.leftPos + deltaX
        val targetTopPos = selected.topPos + deltaY
        val targetRightPos = selected.rightPos + deltaX
        val targetBottomPos = selected.bottomPos + deltaY
        run {
            // we can improve this
            visibleItemsInfo.fastForEach { visibleItem ->
                if (
                    visibleItem.itemIndex == selected.itemIndex ||
                    visibleItem.leftPos > targetRightPos ||
                    visibleItem.topPos > targetBottomPos ||
                    visibleItem.rightPos < targetLeftPos ||
                    visibleItem.bottomPos < targetTopPos
                ) {
                    return@fastForEach
                }
                if (
                    movable?.invoke(
                        ItemPosition(selected.index, selected.key),
                        ItemPosition(visibleItem.index, visibleItem.key)
                    ) == true
                ) {
                    _draggingDropTarget = selected
                }
                return@run
            }
        }
        return _draggingDropTarget
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