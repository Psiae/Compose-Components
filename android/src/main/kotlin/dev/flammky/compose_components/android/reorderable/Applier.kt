package dev.flammky.compose_components.android.reorderable

import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastFirstOrNull
import dev.flammky.compose_components.core.SnapshotReader

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
                        }?.takeIf {
                            // check if the state allow the drag
                            state.onStartDrag(it.position.x.toInt(), it.position.y.toInt())
                        }?.let {
                            if (dragStart.offset != Offset.Zero) {
                                // report initial drag offset
                                state.onDrag(dragStart.offset.x.toInt(), dragStart.offset.y.toInt())
                            }
                            val dragCompleted = drag(dragStart.id) { change ->
                                // report each drag position change
                                state.onDrag(change.position.x.toInt(), change.position.y.toInt())
                            }
                            if (dragCompleted) {
                                // completed normally
                                state.onDragEnd()
                            } else {
                                // was cancelled
                                state.onDragCancelled()
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