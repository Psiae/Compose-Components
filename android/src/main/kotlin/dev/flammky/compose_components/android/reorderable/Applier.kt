package dev.flammky.compose_components.android.reorderable

import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.derivedStateOf
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

    fun onLazyListScope(
        lazyListScope: LazyListScope,
        itemProvider: ReorderableLazyListItemProvider
    )

    fun onStartReorder(from: ItemPosition): Boolean
    fun onMove(from: ItemPosition, new: ItemPosition): Boolean
    fun onEndReorder(from: ItemPosition, to: ItemPosition)
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

    private var _currentComposition by mutableStateOf<ReorderableLazyListItemProvider?>(null)
    
    private val pointerInputFilterModifier: Modifier = Modifier.pointerInput(Unit) {
        // for each possible gesture, install child drag listener
        forEachGesture {
            // await first DragStart, for each gesture we only accept the first emission
            state.childReorderStartChannel.receive()
                .let { dragStart ->
                    val slop = dragStart.slop
                    // received DragStart, create pointer event awaiter on this composable
                    awaitPointerEventScope {
                        // find the event to get the position in the parent
                        currentEvent.changes.fastFirstOrNull { pointerInputChange ->
                            pointerInputChange.id == dragStart.id
                        }?.takeIf { pointer ->
                            // check if the state allow the drag
                            state.onStartDrag(
                                pointer.id.value,
                                pointer.position.x - slop.x,
                                pointer.position.y - slop.y,
                                slop.x,
                                slop.y,
                                expectKey = dragStart.selfKey,
                                expectIndex = dragStart.selfIndex
                            )
                        }?.let { _ ->
                            val lastDragId: PointerId = dragStart.id
                            var lastDragX: Float = dragStart.slop.x
                            var lastDragY: Float = dragStart.slop.y
                            val dragCompleted =
                                try {
                                    drag(dragStart.id) { onDrag ->
                                        // report each drag position change
                                        check(lastDragId == onDrag.id)
                                        lastDragX = onDrag.position.x
                                        lastDragY = onDrag.position.y
                                        val change = onDrag.positionChange()
                                        val accepted = state.onDrag(
                                            lastDragId.value,
                                            change.x,
                                            change.y,
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

    override fun onLazyListScope(
        lazyListScope: LazyListScope,
        itemProvider: ReorderableLazyListItemProvider
    ) {
        itemProvider.provideLayout(lazyListScope)
    }

    override fun onStartReorder(from: ItemPosition): Boolean {
        return true
    }

    override fun onMove(from: ItemPosition, new: ItemPosition): Boolean {
        return true
    }

    override fun onEndReorder(from: ItemPosition, to: ItemPosition) {

    }

    @SnapshotRead
    fun indexOfKey(key: Any): Int = _currentComposition?.indexOfKey(key) ?: -1
}