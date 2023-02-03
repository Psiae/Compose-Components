package dev.flammky.compose_components.android.reorderable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import dev.flammky.compose_components.core.SnapshotRead
import dev.flammky.compose_components.core.horizontalOffset
import dev.flammky.compose_components.core.notImplementedError
import dev.flammky.compose_components.core.verticalOffset

interface ReorderableLazyListScope {

    fun item(
        // as of now key is a must, there will be non-key variant in the future
        key: Any,
        content: @Composable ReorderableLazyItemScope.() -> Unit
    ) = items(1, { key }) { content() }

    fun items(
        count: Int,
        // as of now key is a must, there will be non-key variant in the future
        key: (Int) -> Any,
        content: @Composable ReorderableLazyItemScope.(Int) -> Unit
    )
}

interface ReorderableLazyItemScope {

    val info: ItemInfo
    
    fun Modifier.reorderInput(): Modifier
    fun Modifier.reorderLongInput(timeMs: Long? = null): Modifier
    fun Modifier.reorderableItemModifiers(): Modifier

    interface ItemInfo {
        val dragging: Boolean
            @SnapshotRead get

        val key: Any

        val indexInParent: Int
    }
}

internal class RealReorderableLazyListScope(
    private val state: ReorderableLazyListState,
    private val lazyListScope: LazyListScope
) : ReorderableLazyListScope {

    private var itemCount = 0

    private val orientation =
        if (state.isVerticalScroll)
            Orientation.Vertical
        else if (state.isHorizontalScroll)
            Orientation.Horizontal
        else notImplementedError()

    override fun item(key: Any, content: @Composable ReorderableLazyItemScope.() -> Unit) {
        val index = itemCount++
        lazyListScope.item(
            key = key
        ) {
            with(
                receiver = remember {
                    RealReorderableLazyItemScope(
                        parentOrientation = orientation,
                        itemPositionInParent = ItemPosition(index, key),
                        parentDraggingItem = {
                            state.draggingItemPosition
                        },
                        onReorderInput = { pid, offset ->
                            state.childDragStartChannel.trySend(DragStart(pid, offset ?: Offset.Zero))
                        }
                    )
                }
            ) {
                content()
            }
        }
    }

    override fun items(
        count: Int,
        key: (Int) -> Any,
        content: @Composable ReorderableLazyItemScope.(Int) -> Unit
    ) {
        val index = itemCount
        itemCount += count
        lazyListScope.items(
            count = count,
            key = key,
        ) { i ->
            with(
                receiver = remember {
                    RealReorderableLazyItemScope(
                        parentOrientation = orientation,
                        itemPositionInParent = ItemPosition(index + i, key),
                        parentDraggingItem = {
                            state.draggingItemPosition
                        },
                        onReorderInput = { pid, offset ->
                            state.childDragStartChannel.trySend(DragStart(pid, offset ?: Offset.Zero))
                        }
                    )
                }
            ) {
                content(i)
            }
        }
    }
}

internal class RealReorderableLazyItemScope(
    private val parentOrientation: Orientation,
    private val itemPositionInParent: ItemPosition,
    private val parentDraggingItem: @SnapshotRead () -> ItemPosition?,
    private val onReorderInput: (pointerId: PointerId, offset: Offset?) -> Unit,
) : ReorderableLazyItemScope {

    // make it lazy ?
    override val info = object : ReorderableLazyItemScope.ItemInfo {

        override val dragging: Boolean by derivedStateOf(policy = structuralEqualityPolicy()) {
            parentDraggingItem()?.takeIf { it.index == indexInParent && it.key == key } != null
        }

        override val key: Any = itemPositionInParent.key
        override val indexInParent: Int = itemPositionInParent.index
    }

    override fun Modifier.reorderInput(): Modifier {
        return this.then(
            // install suspending pointer input filter to the composable
            Modifier.pointerInput(Unit) {
                // for each possible gesture, install handle
                forEachGesture {
                    // put pointer filter
                    awaitPointerEventScope {
                        // await first down / press
                        awaitFirstDown().let { firstDown ->
                            // await for pointer slop (a distance in pixel before a gesture is considered a movement)
                            var slop = Offset.Zero
                            val awaitSlop = when (parentOrientation) {
                                Orientation.Vertical -> {
                                    awaitVerticalPointerSlopOrCancellation(
                                        pointerId = firstDown.id,
                                        pointerType = firstDown.type
                                    ) { change, slopReached ->
                                        change.consume()
                                        slop = verticalOffset(slopReached)
                                    }
                                }
                                Orientation.Horizontal -> {
                                    awaitHorizontalPointerSlopOrCancellation(
                                        pointerId = firstDown.id,
                                        pointerType = firstDown.type
                                    ) { change, slopReached ->
                                        change.consume()
                                        slop = horizontalOffset(slopReached)
                                    }
                                }
                                else -> notImplementedError()
                            }
                            if (awaitSlop != null) {
                                // if the slop is reached from the down event then it's a Drag Start
                                onReorderInput(firstDown.id, slop)
                            }
                        }
                    }
                }
            }
        )
    }

    override fun Modifier.reorderLongInput(timeMs: Long?): Modifier {
        return this.then(
            // install pointer-input filter to the composable
            Modifier.pointerInput(Unit) {
                // for each new gesture, install handle
                forEachGesture {
                    // put pointerEvent filter
                    awaitPointerEventScope {
                        // await first down / gesture
                        awaitFirstDown()
                            .let { firstDown ->
                                // await the down event for the specified time
                                if (awaitLongPressOrCancellation(firstDown.id, timeMs) != null) {
                                    // was pressed long enough without interruption then it's a Drag Start
                                    onReorderInput(firstDown.id, null)
                                }
                            }
                    }
                }
            }
        )
    }

    override fun Modifier.reorderableItemModifiers(): Modifier {
        return this.then(Modifier)
    }
}

private data class ReorderStartInput(
    val change: PointerInputChange,
    val slop: Offset
)