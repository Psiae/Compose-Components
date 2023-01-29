package dev.flammky.compose_components.android.reorderable.lazylist

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ReorderableLazyListScope {

    val lazyListScope: LazyListScope

    fun item(
        key: Any?,
        content: @Composable ReorderableLazyItemScope.() -> Unit
    ) = items(1) { content() }

    fun items(
        count: Int,
        key: (Int) -> Any? = { null },
        content: @Composable ReorderableLazyItemScope.(Int) -> Unit
    )
}

interface ReorderableLazyItemScope {
    fun Modifier.reorderable(): Modifier
}