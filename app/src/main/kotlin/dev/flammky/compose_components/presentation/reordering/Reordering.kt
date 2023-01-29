package dev.flammky.compose_components.presentation.reordering

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.flammky.compose_components.android.reorderable.ItemPosition
import dev.flammky.compose_components.android.reorderable.lazylist.ReorderableLazyColumn
import dev.flammky.compose_components.android.reorderable.lazylist.ReorderableLazyItemScope
import dev.flammky.compose_components.android.reorderable.lazylist.rememberReorderableLazyListState
import dev.flammky.compose_components.core.NoInline

@Composable
internal fun Ordering(
    viewModel: ReorderingViewModel = viewModel()
) {

    val statusBarHeight = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }

    val navigationBarHeight = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    NoInline {
        val currentList = viewModel.taskListState

        if (currentList.value.currentTaskIndex < 0) {
            return@NoInline
        }

        val lazyListState = rememberLazyListState(
            initialFirstVisibleItemIndex = currentList.value.currentTaskIndex,
            initialFirstVisibleItemScrollOffset = statusBarHeight.value.toInt()
        )
        
        ReorderableLazyColumn(
            state = rememberReorderableLazyListState(
                lazyListState = lazyListState,
                onDragStart = { item: ItemPosition ->

                },
                onDragEnd = { _, _ ->

                },
                canDragOverItem = { _, _ -> true },
                onMove = move@ { from, to ->
                    // allow smart-cast
                    val fromKey = from.key
                    val toKey = to.key
                    if (fromKey !is ItemPositionKey || toKey !is ItemPositionKey) {
                        // should stress-test
                        return@move
                    }
                    if (fromKey.listSnapshotID == toKey.listSnapshotID) {
                        viewModel.moveTask(
                            snapshotListID = fromKey.listSnapshotID,
                            from = from.index,
                            to = to.index
                        )
                    }
                }
            ),
            contentPadding = PaddingValues(
                top = statusBarHeight,
                bottom = navigationBarHeight
            ),
        ) scope@ {
            val maskedList = viewModel.maskedTaskListState.value
            items(
                count = maskedList.list.size,
                key = { i -> maskedList.list[i].id }
            ) { i ->
                TaskItemLayout(viewModel, maskedList.list[i])
            }
        }
    }

    DisposableEffect(key1 = viewModel, effect = {
        val obs = viewModel.observeTask()
        onDispose { obs.cancel() }
    })
}

@Composable
private fun ReorderableLazyItemScope.TaskItemLayout(
    viewModel: ReorderingViewModel,
    item: TaskList.Item
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .reorderable()
    ) {

    }
}

private data class ItemPositionKey(
    val snapshotID: String,
    val listSnapshotID: String,
)