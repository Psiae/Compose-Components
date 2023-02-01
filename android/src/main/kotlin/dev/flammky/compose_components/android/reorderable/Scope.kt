package dev.flammky.compose_components.android.reorderable

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import dev.flammky.compose_components.android.reorderable.leech.awaitLongPressOrCancellation
import dev.flammky.compose_components.core.SnapshotRead
import dev.flammky.compose_components.core.horizontalOffset
import dev.flammky.compose_components.core.verticalOffset

interface ReorderableLazyListScope {

    val lazyListScope: LazyListScope

    fun item(
        // as of now key is a must, there will be non-key variant in the future
        key: Any,
        content: @Composable ReorderableLazyItemScope.() -> Unit
    ) = items(1, { key }) { content() }

    fun items(
        // as of now key is a must, there will be non-key variant in the future
        count: Int,
        key: (Int) -> Any,
        content: @Composable ReorderableLazyItemScope.(Int) -> Unit
    )
}

interface ReorderableLazyItemScope {

    val dragging: Boolean
        @SnapshotRead get

    val key: Any?
        @SnapshotRead get

    val index: Int
        @SnapshotRead get
    
    fun Modifier.reorderInput(): Modifier
    fun Modifier.reorderLongInput(timeMs: Long? = null): Modifier
    fun Modifier.reorderableItemModifiers(): Modifier
}

internal class RealReorderableLazyListScope(
    private val state: ReorderableState<*>
) : ReorderableLazyListScope {

    override val lazyListScope: LazyListScope
        get() = TODO("Not yet implemented")

    override fun item(key: Any, content: @Composable ReorderableLazyItemScope.() -> Unit) {
        super.item(key, content)
    }

    override fun items(
        count: Int,
        key: (Int) -> Any,
        content: @Composable ReorderableLazyItemScope.(Int) -> Unit
    ) {
        TODO("Not yet implemented")
    }
}

internal class RealReorderableLazyItemScope(
    private val parentOrientation: Orientation,
    private val onReorderInput: (pointerId: PointerId, offset: Offset?) -> Unit
) : ReorderableLazyItemScope {

    override val dragging: Boolean
        @SnapshotRead
        get() = TODO("Not yet implemented")

    override val key: Any
        @SnapshotRead
        get() = TODO("Not yet implemented")

    override val index: Int
        @SnapshotRead
        get() = TODO("Not yet implemented")

    override fun Modifier.reorderInput(): Modifier {
        return this.then(
            // install suspending pointer input filter to the composable
            Modifier.pointerInput(Unit) {
                // for each new gesture, install handle
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
                                else -> error("Not Implemented Error")
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
                    }.let { firstDown ->
                        // await the down event for the specified time
                        if (awaitLongPressOrCancellation(firstDown, timeMs) == firstDown) {
                            // was pressed long enough without interruption then it's a Drag Start
                            onReorderInput(firstDown.id, null)
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