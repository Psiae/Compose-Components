package dev.flammky.compose_components.presentation.reordering

import android.os.Parcel
import android.os.Parcelable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.flammky.compose_components.android.R
import dev.flammky.compose_components.android.reorderable.ItemPosition
import dev.flammky.compose_components.android.reorderable.lazylist.ReorderableLazyColumn
import dev.flammky.compose_components.android.reorderable.lazylist.ReorderableLazyItemScope
import dev.flammky.compose_components.android.reorderable.lazylist.rememberReorderableLazyListState
import dev.flammky.compose_components.android.reorderable.leech.*
import dev.flammky.compose_components.core.NoInline
import dev.flammky.compose_components.presentation.theme.Theme
import dev.flammky.compose_components.presentation.theme.backgroundContentColorAsState

@Composable
internal fun Ordering(
    viewModel: ReorderingViewModel = viewModel()
) {

}

@Composable
private fun OrderingTestUsage(
    viewModel: ReorderingViewModel
) {
    val statusBarHeight = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }

    val navigationBarHeight = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    NoInline {
        val currentList = viewModel.actualTaskListState

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
                onDragStart = start@ { item: ItemPosition ->
                    val id = (item.key as? TaskItemPositionKey)?.listSnapshotID
                        ?: return@start
                    viewModel.startMoveTask(id, item.index)
                },
                onDragEnd = { cancelled, _, _ ->
                    if (!cancelled) {
                        viewModel.commitMoveTask()
                    } else {
                        viewModel.cancelMoveTask()
                    }
                },
                canDragOverItem = { _, _ -> true },
                onMove = move@ { from, to ->
                    // allow smart-cast
                    val fromKey = from.key
                    val toKey = to.key
                    if (fromKey !is TaskItemPositionKey || toKey !is TaskItemPositionKey) {
                        // should stress-test
                        return@move
                    }
                    viewModel.moveTask(
                        snapshotListID = fromKey.listSnapshotID,
                        from = from.index,
                        to = to.index
                    )
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
                TestTaskItemLayout(maskedList.list[i])
            }
        }
    }

    DisposableEffect(key1 = viewModel, effect = {
        val obs = viewModel.observeTask()
        onDispose { obs.cancel() }
    })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LazyItemScope.LeechTestTaskItemLayout(
    state: ReorderableState<*>,
    item: TaskList.Item,
    index: Int
) {
    ReorderableItem(reorderableState = state, null, index = index) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(24.dp)
                    .detectReorder(state),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.drag_handle_fill0_wght400_grad0_opsz48),
                    contentDescription = "drag_handle",
                    tint = Theme.backgroundContentColorAsState().value
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = item.id,
                    color = Theme.backgroundContentColorAsState().value
                )
            }
        }
    }
}

@Composable
private fun ReorderableLazyItemScope.TestTaskItemLayout(
    item: TaskList.Item
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .reorderableItemModifiers(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(24.dp)
                .reorderInput(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.drag_handle_fill0_wght400_grad0_opsz48),
                contentDescription = "drag_handle",
                tint = Theme.backgroundContentColorAsState().value
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = item.id,
                color = Theme.backgroundContentColorAsState().value
            )
        }
    }
}

internal data class TaskItemPositionKey(
    val snapshotID: String,
    val listSnapshotID: String,
    val id: String
) : Parcelable {
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(snapshotID)
        dest.writeString(listSnapshotID)
        dest.writeString(id)
    }

    companion object {

        @JvmField
        @Suppress("unused")
        val CREATOR = object : Parcelable.Creator<TaskItemPositionKey> {

            override fun createFromParcel(source: Parcel): TaskItemPositionKey {
                return TaskItemPositionKey(
                    source.readString()!!,
                    source.readString()!!,
                    source.readString()!!
                )
            }

            override fun newArray(size: Int): Array<TaskItemPositionKey?> {
                return arrayOfNulls<TaskItemPositionKey?>(size)
            }
        }
    }
}