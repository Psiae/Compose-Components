package dev.flammky.compose_components.android.reorderable

import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull
import dev.flammky.compose_components.core.SnapshotRead
import dev.flammky.compose_components.core.SnapshotReader
import kotlinx.coroutines.CancellationException

internal interface ReorderableLazyListApplier {

    val lazyLayoutModifiers: Modifier

    fun onRecomposeContent(
        lazyListScope: LazyListScope,
        content: @SnapshotReader ReorderableLazyListScope.() -> Unit
    )
}

/*@Composable
internal fun rememberReorderableLazyListApplier(
    state: ReorderableLazyListState
): ReorderableLazyListApplier {
    val applier = remember(state) {
        RealReorderableLazyListApplier(state).apply { acquireState() }
    }
    DisposableEffect(
        key1 = applier,
        effect = {
            onDispose { applier.releaseState() }
        }
    )
    return applier
}*/

internal class RealReorderableLazyListApplier(
    private val state: ReorderableLazyListState
) : ReorderableLazyListApplier {

    private var _currentComposition by mutableStateOf<RealReorderableLazyListScope?>(null)
    
    private val pointerInputFilterModifier: Modifier = Modifier.pointerInput(Unit) {
        // for each possible gesture, install child drag listener
        forEachGesture {
            // await first DragStart, for each gesture we only accept the first emission
            state.childReorderStartChannel.receive()
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
                                firstDown.position.y.toInt(),
                                expectKey = dragStart.selfKey,
                                expectIndex = dragStart.selfIndex
                            )
                        }?.let { _ ->
                            val lastDragId: PointerId = dragStart.id
                            var lastDragX: Int = dragStart.offset.x.toInt()
                            var lastDragY: Int = dragStart.offset.y.toInt()
                            val dragCompleted =
                                try {
                                    drag(dragStart.id) { onDrag ->
                                        // report each drag position change
                                        check(lastDragId == onDrag.id)
                                        lastDragX = onDrag.position.x.toInt()
                                        lastDragY = onDrag.position.y.toInt()
                                        val change = onDrag.positionChange()
                                        val accepted = state.onDrag(
                                            lastDragId.value,
                                            change.x.toInt(),
                                            change.y.toInt(),
                                            expectKey = dragStart.selfKey
                                        )
                                        if (!accepted) throw CancellationException()
                                    }
                                } catch (ce: CancellationException) {
                                    false
                                }
                            if (dragCompleted) {
                                // completed normally
                                state.onDragEnd(
                                    id = lastDragId.value,
                                    endX = lastDragX,
                                    endY = lastDragY,
                                    expectKey = dragStart.selfKey
                                )
                            } else {
                                // was cancelled
                                state.onDragCancelled(
                                    id = lastDragId.value,
                                    endX = lastDragX,
                                    endY = lastDragY,
                                    expectKey = dragStart.selfKey
                                )
                            }
                        }
                    }
                }
        }
    }

    override val lazyLayoutModifiers: Modifier = pointerInputFilterModifier

    override fun onRecomposeContent(
        lazyListScope: LazyListScope,
        content: @SnapshotReader ReorderableLazyListScope.() -> Unit
    ) {
        _currentComposition = RealReorderableLazyListScope(
            state = state,
            lazyListScope = lazyListScope
        ).apply(content)
    }

    @SnapshotRead
    fun indexOfKey(key: Any): Int = _currentComposition?.indexOfKey(key) ?: -1
}