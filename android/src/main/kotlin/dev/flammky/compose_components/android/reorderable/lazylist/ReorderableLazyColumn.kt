package dev.flammky.compose_components.android.reorderable.lazylist

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

interface ReorderableLazyColumnScope {
    fun item(
        key: (index: Int) -> Any?,
        content: @Composable ReorderableLazyColumnItemScope.() -> Unit
    )
    fun items(
        count: Int,
        key: (index: Int) -> Any?,
        content: @Composable ReorderableLazyColumnScope.() -> Unit
    )
}

interface ReorderableLazyColumnItemScope {

}

internal class RealReorderableLazyColumnScope : ReorderableLazyColumnScope {

    override fun item(
        key: (index: Int) -> Any?,
        content: ReorderableLazyColumnItemScope.() -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun items(
        count: Int,
        key: (index: Int) -> Any?,
        content: ReorderableLazyColumnScope.() -> Unit
    ) {
        TODO("Not yet implemented")
    }
}

internal class RealReorderableItemScope : ReorderableLazyColumnItemScope {

}

@Composable
fun ReorderableLazyColumn(
    modifier: Modifier = Modifier,
    state: ReorderableLazyColumnState,
    content: ReorderableLazyColumnScope.() -> Unit
) {
}

@Composable
fun rememberReorderableLazyColumnState(
    lazyColumnState: LazyListState = rememberLazyListState()
): ReorderableLazyColumnState {

    val state = remember(lazyColumnState) {
        ReorderableLazyColumnState(lazyColumnState)
    }

    val coroutineScope = rememberCoroutineScope()

    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    return state
}