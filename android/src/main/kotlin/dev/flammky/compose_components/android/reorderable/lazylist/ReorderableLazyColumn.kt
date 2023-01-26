package dev.flammky.compose_components.android.reorderable.lazylist

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.flammky.compose_components.core.SnapshotReader

@Composable
fun ReorderableLazyColumn(
    modifier: Modifier = Modifier,
    state: ReorderableLazyListState,
    content: @SnapshotReader ReorderableLazyListScope.() -> Unit
) {

    val applier: ReorderableLazyListApplier = rememberReorderableLazyListApplier(state)

    LazyColumn(
        modifier = modifier,
        state = state.lazyListState,
        content = {
            with(applier) { apply(content) }
        }
    )
}