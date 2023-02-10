package dev.flammky.compose_components.android.reorderable

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.*
import dev.flammky.compose_components.core.SnapshotReader

internal class ReorderableLazyListItemProvider(
    private val state: ReorderableLazyListState,
    private val baseContentState: State<@SnapshotReader ReorderableLazyListScope.() -> Unit>
) {

    private val baseScope by derivedStateOf {
        RealReorderableLazyListScope(state).apply(baseContentState.value)
    }

    fun provideLayout(lazyListScope: LazyListScope) {
        baseScope.intervals.forEach { interval ->
            lazyListScope.items(
                interval.items.size,
                { i -> interval.items[i].key },
                { i -> interval.items[i].type },
            ) { i ->
                val composition = baseScope
                rememberInternalReorderableLazyItemScope(
                    composition = composition,
                    displayIndex = i
                ).run {
                    ComposeContent()
                }
            }
        }
    }

    fun indexOfKey(key: Any): Int = baseScope.indexOfKey(key)
}

@Composable
internal fun rememberReorderableLazyListItemProvider(
    state: ReorderableLazyListState,
    baseContent: @SnapshotReader ReorderableLazyListScope.() -> Unit
): ReorderableLazyListItemProvider {

    val updatedBaseContent = rememberUpdatedState(newValue = baseContent)

    return remember(state) {
        ReorderableLazyListItemProvider(state, updatedBaseContent)
    }
}