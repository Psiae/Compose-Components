package dev.flammky.compose_components.presentation.ordering

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.flammky.compose_components.android.reorderable.lazylist.ReorderableLazyColumn
import dev.flammky.compose_components.android.reorderable.lazylist.rememberReorderableLazyColumnState

@Composable
fun Ordering() {

    ReorderableLazyColumn(
        modifier = Modifier,
        state = rememberReorderableLazyColumnState(),
    ) {

    }
}