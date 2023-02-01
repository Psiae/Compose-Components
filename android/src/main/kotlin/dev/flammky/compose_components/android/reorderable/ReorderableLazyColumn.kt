package dev.flammky.compose_components.android.reorderable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.flammky.compose_components.core.SnapshotReader

@Composable
fun ReorderableLazyColumn(
    modifier: Modifier = Modifier,
    state: ReorderableLazyListState,
    content: @SnapshotReader ReorderableLazyListScope.() -> Unit
) = ReorderableLazyColumn(
    modifier = modifier,
    state = state,
    contentPadding = PaddingValues(),
    content = content
)

@Composable
fun ReorderableLazyColumn(
    modifier: Modifier = Modifier,
    state: ReorderableLazyListState,
    contentPadding: PaddingValues,
    content: @SnapshotReader ReorderableLazyListScope.() -> Unit
) {

    val applier: ReorderableLazyListApplier = rememberReorderableLazyListApplier(state)

    LazyColumn(
        modifier = modifier.then(applier.pointerInputFilterModifier),
        state = state.lazyListState,
        contentPadding = contentPadding
    ) scope@ {
        applier.apply(this@scope, content)
    }
}