package dev.flammky.compose_components.android.reorderable.lazylist

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.flammky.compose_components.core.SnapshotRead

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

    val dragging: Boolean
        @SnapshotRead get

    val key: Any?
        @SnapshotRead get

    val index: Int
        @SnapshotRead get
    
    fun Modifier.reorderInput(): Modifier
    fun Modifier.reorderLongInput(timeout: Long): Modifier
    fun Modifier.reorderableItemModifiers(): Modifier
}