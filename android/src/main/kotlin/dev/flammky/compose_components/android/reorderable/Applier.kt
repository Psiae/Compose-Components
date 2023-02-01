package dev.flammky.compose_components.android.reorderable

import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
    state: ReorderableLazyListState
) : ReorderableLazyListApplier {

    override val pointerInputFilterModifier: Modifier = Modifier.pointerInput(Unit) {
        forEachGesture {
            state.childDragStartChannel.receive().let { dragStart ->
                awaitPointerEventScope {
                    currentEvent.changes.fastFirstOrNull { pointerInputChange ->
                        pointerInputChange.id == dragStart.id
                    }
                }?.let { validPress ->
                    if (state.onStartDrag(validPress.position.x.toInt(), validPress.position.y.toInt())) {
                        dragStart.offset
                            ?.run {
                                state.onDrag(x.toInt(), y.toInt())
                            }
                        awaitPointerEventScope {
                            drag(dragStart.id) { change ->
                                state.onDrag(change.position.x.toInt(), change.position.y.toInt())
                            }.let { completeNormally ->
                                if (completeNormally) {
                                    state.onDragEnd()
                                } else {
                                    state.onDragCancelled()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun apply(lazyListScope: LazyListScope, content: ReorderableLazyListScope.() -> Unit) {
        lazyListScope.item {  }
    }
}