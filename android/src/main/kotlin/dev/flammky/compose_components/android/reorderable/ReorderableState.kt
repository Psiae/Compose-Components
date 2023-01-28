package dev.flammky.compose_components.android.reorderable

import dev.flammky.compose_components.android.reorderable.lazylist.ReorderableLazyListApplier
import dev.flammky.compose_components.core.SnapshotRead
import kotlinx.coroutines.channels.Channel

abstract class ReorderableState <ItemInfo> internal constructor() {

    //
    // Promote accordingly
    //

    /**
     * Coroutine Channel for reorderable childrens to report `drag-start` event to the layout handler
     *
     * @see [ReorderableLazyListApplier.pointerInputFilterModifier]
     */
    internal abstract val childDragStartChannel: Channel<DragStart>

    /**
     * The Index of the currently dragging Item or null if none is dragged.
     * State Change within the getter will notify SnapshotObserver that reads it
     */
    abstract val draggingItemIndex: Int?
        @SnapshotRead
        get

    /**
     * The Key of the currently dragging Item or null if none is dragged.
     * State Change within the getter will notify SnapshotObserver that reads it
     *
     */
    abstract val draggingItemKey: Any?
        @SnapshotRead
        get

    protected abstract val firstVisibleItemIndex: Int
        @SnapshotRead
        get

    protected abstract val firstVisibleItemScrollOffset: Int
        @SnapshotRead
        get

    protected abstract val viewportStartOffset: Int
        @SnapshotRead
        get

    protected abstract val viewportEndOffset: Int
        @SnapshotRead
        get

    /**
     * The Index of the Layout Item within the Reorderable Scope
     */
    protected abstract val ItemInfo.itemIndex: Int

    /**
     * The Key of the Layout Item within the Reorderable Scope
     */
    protected abstract val ItemInfo.itemKey: Any

    /**
     * The Left Position of The Item relative to the Parent Layout ViewPort
     */
    protected abstract val ItemInfo.leftPos: Int
        @SnapshotRead
        get

    /**
     * The Right Position of The Item relative to the Parent Layout ViewPort
     */
    protected abstract val ItemInfo.rightPos: Int
        @SnapshotRead
        get

    /**
     * The Top Position of The Item relative to the Parent Layout ViewPort
     */
    protected abstract val ItemInfo.topPos: Int
        @SnapshotRead
        get

    /**
     * The Bottom Position of The Item relative to the Parent Layout ViewPort
     */
    protected abstract val ItemInfo.bottomPos: Int
        @SnapshotRead
        get

    /**
     * The Height of the Item,
     */
    protected abstract val ItemInfo.height: Int
        @SnapshotRead
        get

    /**
     * The Width of the Item,
     */
    protected abstract val ItemInfo.width: Int
        @SnapshotRead
        get

    internal abstract fun onStartDrag(startX: Int, startY: Int): Boolean
    internal abstract fun onDrag(dragX: Int, dragY: Int)
    internal abstract fun onDragEnd()
    internal abstract fun onDragCancelled()
}