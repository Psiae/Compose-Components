package dev.flammky.compose_components.android.reorderable.lazylist

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.flammky.compose_components.core.SnapshotReader

interface ReorderableLazyListApplier {
    fun LazyListScope.apply(content: @SnapshotReader ReorderableLazyListScope.() -> Unit)
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

    override fun LazyListScope.apply(content: ReorderableLazyListScope.() -> Unit) {
        TODO("Not yet implemented")
    }
}