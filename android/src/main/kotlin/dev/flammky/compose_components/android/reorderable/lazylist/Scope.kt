package dev.flammky.compose_components.android.reorderable.lazylist

import android.view.ViewConfiguration
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.flammky.compose_components.core.SnapshotRead

interface ReorderableLazyListScope {

    val lazyListScope: LazyListScope

    fun item(
        // as of now key is a must, there will be non-key variant in the future
        key: Any,
        content: @Composable ReorderableLazyItemScope.() -> Unit
    ) = items(1, { key }) { content() }

    fun items(
        // as of now key is a must, there will be non-key variant in the future
        count: Int,
        key: (Int) -> Any,
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
    fun Modifier.reorderLongInput(time: Int = ViewConfiguration.getLongPressTimeout()): Modifier
    fun Modifier.reorderableItemModifiers(): Modifier
}