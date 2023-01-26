package dev.flammky.compose_components.android.reorderable

import dev.flammky.compose_components.core.SnapshotRead
import kotlinx.coroutines.channels.Channel

abstract class ReorderableState <ItemInfo> internal constructor() {

    //
    // Promote accordingly
    //

    internal abstract val childDragChannel: Channel<DragStart>

    abstract val draggingItemIndex: Int?
        @SnapshotRead
        get

    abstract val draggingItemKey: Any?
        @SnapshotRead
        get

    abstract val ItemInfo.itemIndex: Int
    abstract val ItemInfo.itemKey: Any

    abstract val ItemInfo.startRelativePos: Int
        @SnapshotRead
        get
    abstract val ItemInfo.endRelativePos: Int
        @SnapshotRead
        get
    abstract val ItemInfo.topRelativePos: Int
        @SnapshotRead
        get
    abstract val ItemInfo.bottomRelativePos: Int
        @SnapshotRead
        get
}