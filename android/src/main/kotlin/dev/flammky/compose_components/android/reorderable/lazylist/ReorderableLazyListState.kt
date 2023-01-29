package dev.flammky.compose_components.android.reorderable.lazylist

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.fastFirstOrNull
import dev.flammky.compose_components.android.reorderable.DragStart
import dev.flammky.compose_components.android.reorderable.ItemPosition
import dev.flammky.compose_components.android.reorderable.ReorderableScrollableState
import dev.flammky.compose_components.core.SnapshotRead
import kotlinx.coroutines.channels.Channel

class ReorderableLazyListState internal constructor(
    val lazyListState: LazyListState,
    val canDragOver: (from: ItemPosition, to: ItemPosition) -> Boolean
) : ReorderableScrollableState<LazyListItemInfo>() {

    private var _selectedItem by mutableStateOf<LazyListItemInfo?>(null)
    private var _draggingDelta by mutableStateOf<Offset>(Offset.Zero)

    private val targets = mutableListOf<LazyListItemInfo>()
    private val distances = mutableListOf<Int>()

    private val _draggingItemLayoutInfo: LazyListItemInfo?
        get() = visibleItemsInfo.fastFirstOrNull { visibleItem ->
            visibleItem.itemIndex == draggingItemIndex
        }

    internal override val childDragStartChannel: Channel<DragStart> = Channel()
    internal override val scrollChannel: Channel<Float> = Channel()

    override val isVerticalScroll: Boolean
        @SnapshotRead
        get() = lazyListState.layoutInfo.orientation == Orientation.Vertical

    override val visibleItemsInfo: List<LazyListItemInfo>
        @SnapshotRead
        get() = lazyListState.layoutInfo.visibleItemsInfo

    override val reverseLayout: Boolean
        @SnapshotRead
        get() = lazyListState.layoutInfo.reverseLayout

    override var draggingItemIndex: Int? by mutableStateOf(null)
        @SnapshotRead
        get

    override val draggingItemKey: Any?
        @SnapshotRead
        get() = _selectedItem?.itemKey

    override val firstVisibleItemIndex: Int
        get() = lazyListState.firstVisibleItemIndex

    override val firstVisibleItemScrollOffset: Int
        get() = lazyListState.firstVisibleItemScrollOffset

    override val viewportStartOffset: Int
        get() = lazyListState.layoutInfo.viewportStartOffset

    override val viewportEndOffset: Int
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

    override fun onStartDrag(startX: Int, startY: Int): Boolean {
        val x: Int
        val y: Int
        if (isVerticalScroll) {
            x = startX
            y = viewportStartOffset + startY
        } else {
            x = viewportStartOffset + startX
            y = startY
        }
        return visibleItemsInfo
            .fastFirstOrNull {
                x in it.leftPos..it.rightPos && y in it.topPos..it.bottomPos
            }
            ?.also { itemInfo ->
                _selectedItem = itemInfo
                draggingItemIndex = itemInfo.itemIndex
            } != null
    }

    override fun onDrag(dragX: Int, dragY: Int) {
        TODO("Not yet implemented")
    }

    override fun onDragEnd() {
        TODO("Not yet implemented")
    }

    override fun onDragCancelled() {
        TODO("Not yet implemented")
    }
}

@Composable
fun rememberReorderableLazyListState(
    lazyListState: LazyListState,
    onDragStart: (/*snapshot: Any?,*/ item: ItemPosition) -> Unit,
    canDragOverItem: (/*snapshot: Any?,*/ item: ItemPosition, dragging: ItemPosition) -> Boolean,
    onDragEnd: (/*snapshot: Any?,*/ from: ItemPosition, to: ItemPosition) -> Unit,
    onMove: (/*snapshot: Any?,*/ from: ItemPosition, to: ItemPosition) -> Unit,
): ReorderableLazyListState {
    return remember(lazyListState) {
        ReorderableLazyListState(
            lazyListState = lazyListState,
            canDragOver = canDragOverItem
        )
    }
}