package dev.flammky.compose_components.android.reorderable.lazylist

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.flammky.compose_components.android.reorderable.DragStart
import dev.flammky.compose_components.android.reorderable.ReorderableScrollableState
import dev.flammky.compose_components.core.SnapshotRead
import kotlinx.coroutines.channels.Channel

class ReorderableLazyListState internal constructor(
    val lazyListState: LazyListState
) : ReorderableScrollableState<LazyListItemInfo>() {

    private val selectedItem by mutableStateOf<LazyListItemInfo?>(null)

    override val childDragChannel: Channel<DragStart> = Channel()


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
        get() = selectedItem?.itemKey

    override val LazyListItemInfo.itemIndex: Int
        get() = index

    override val LazyListItemInfo.itemKey: Any
        get() = key

    override val LazyListItemInfo.startRelativePos: Int
        @SnapshotRead
        get() = when {
            isVerticalScroll -> 0
            lazyListState.layoutInfo.reverseLayout -> {
                lazyListState.layoutInfo.viewportSize.width - offset - size
            }
            else -> offset
        }

    override val LazyListItemInfo.endRelativePos: Int
        @SnapshotRead
        get() = when {
            isVerticalScroll -> 0
            lazyListState.layoutInfo.reverseLayout -> {
                lazyListState.layoutInfo.viewportSize.width - offset
            }
            else -> offset + size
        }

    override val LazyListItemInfo.topRelativePos: Int
        @SnapshotRead
        get() = when {
            !isVerticalScroll -> 0
            lazyListState.layoutInfo.reverseLayout -> {
                lazyListState.layoutInfo.viewportSize.height - offset - size
            }
            else -> offset
        }

    override val LazyListItemInfo.bottomRelativePos: Int
        @SnapshotRead
        get() = when {
            !isVerticalScroll -> 0
            lazyListState.layoutInfo.reverseLayout -> {
                lazyListState.layoutInfo.viewportSize.height - offset
            }
            else -> offset + size
        }
}