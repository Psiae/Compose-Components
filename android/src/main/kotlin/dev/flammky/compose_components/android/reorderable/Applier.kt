package dev.flammky.compose_components.android.reorderable

import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastFirstOrNull
import dev.flammky.compose_components.core.SnapshotReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

interface ReorderableLazyListApplier {

    val pointerInputFilterModifier: Modifier
    fun apply(
        lazyListScope: LazyListScope,
        content: @SnapshotReader ReorderableLazyListScope.() -> Unit
    )
}

@Composable
internal fun rememberReorderableLazyListApplier(
    state: ReorderableLazyListState
): ReorderableLazyListApplier {
    return remember(state) {
        RealReorderableLazyListApplier(state)
    }
}

internal class RealReorderableLazyListApplier(
    private val state: ReorderableLazyListState
) : ReorderableLazyListApplier {

    override val pointerInputFilterModifier: Modifier = Modifier.pointerInput(Unit) {
        // for each possible gesture, install child drag listener
        forEachGesture {
            // await first DragStart, for each gesture we only accept the first emission
            state.childDragStartChannel.receive()
                .let { dragStart ->
                    // received DragStart, create pointer event awaiter on this composable
                    awaitPointerEventScope {
                        // find the event to get the position in the parent
                        currentEvent.changes.fastFirstOrNull { pointerInputChange ->
                            pointerInputChange.id == dragStart.id
                        }?.takeIf { firstDown ->
                            // check if the state allow the drag
                            state.onStartDrag(
                                firstDown.id.value,
                                firstDown.position.x.toInt(),
                                firstDown.position.y.toInt()
                            )
                        }?.let { _ ->
                            if (dragStart.offset != Offset.Zero) {
                                // report initial drag offset
                                state.onDrag(dragStart.id.value, dragStart.offset.x.toInt(), dragStart.offset.y.toInt())
                            }
                            val lastDragId: PointerId = dragStart.id
                            var lastDragX: Int = dragStart.offset.x.toInt()
                            var lastDragY: Int = dragStart.offset.y.toInt()
                            val dragCompleted =
                                try {
                                    drag(dragStart.id) { change ->
                                        // report each drag position change
                                        check(lastDragId == change.id)
                                        lastDragX = change.position.x.toInt()
                                        lastDragY = change.position.y.toInt()
                                        val accepted = state.onDrag(
                                            lastDragId.value,
                                            lastDragX,
                                            lastDragY
                                        )
                                        if (!accepted) throw CancellationException()
                                    }
                                } catch (ce: CancellationException) {
                                    false
                                }
                            if (dragCompleted) {
                                // completed normally
                                state.onDragEnd(lastDragId.value, lastDragX, lastDragY)
                            } else {
                                // was cancelled
                                state.onDragCancelled(lastDragId.value, lastDragX, lastDragY)
                            }
                        }
                    }
                }
        }
    }

    override fun apply(
        lazyListScope: LazyListScope,
        content: @SnapshotReader ReorderableLazyListScope.() -> Unit
    ) {
        RealReorderableLazyListScope(state, lazyListScope).apply(content)
    }
}