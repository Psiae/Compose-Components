package dev.flammky.compose_components.presentation.reordering

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.flammky.compose_components.R
import dev.flammky.compose_components.android.reorderable.*
import dev.flammky.compose_components.core.NoInline
import dev.flammky.compose_components.presentation.theme.Theme
import dev.flammky.compose_components.presentation.theme.backgroundColorAsState
import dev.flammky.compose_components.presentation.theme.backgroundContentColorAsState
import dev.flammky.compose_components.presentation.theme.surfaceVariantColorAsState

@Composable
internal fun Ordering(
    viewModel: ReorderingViewModel = viewModel()
) {
    OrderingTestUsage(viewModel = viewModel)
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
        val currentList = viewModel.actualQueueState

        if (currentList.value.currentTrackItemIndex < 0) {
            return@NoInline
        }

        val lazyListState = rememberLazyListState(
            initialFirstVisibleItemIndex = currentList.value.currentTrackItemIndex,
            initialFirstVisibleItemScrollOffset = statusBarHeight.value.toInt()
        )

        ReorderableLazyColumn(
            state = rememberReorderableLazyListState(
                lazyListState = lazyListState,
                onDragStart = start@ { item: ItemPosition ->
                    val key = item.key as? QueueItemPositionKey
                        ?: return@start false
                    viewModel.startMoveTrack(key.qID, item.index, key.idInQueue)
                    true
                },
                onDragEnd = { cancelled, _, _ ->
                    Log.d("Reorderable_DEBUG_Case", "onDragEnd($cancelled)")
                    if (!cancelled) {
                        viewModel.commitMoveQueueItem()
                    } else {
                        viewModel.cancelMoveTask()
                    }
                },
                movable = { from, to ->
                    Log.d("Reorderable_DEBUG_Case", "movable($from, $to)")
                    true
                },
                onMove = move@ { from, to ->
                    Log.d("Reorderable_DEBUG_Case", "onMove($from, $to)")
                    // allow smart-cast
                    val fromKey = from.key
                    val toKey = to.key
                    if (fromKey !is QueueItemPositionKey || toKey !is QueueItemPositionKey) {
                        return@move false
                    }
                    viewModel.moveTask(
                        qId = fromKey.qID
                            .takeIf { it == toKey.qID }
                            ?: return@move false,
                        from = from.index,
                        expectFromID = fromKey.idInQueue,
                        to = to.index,
                        expectToID = toKey.idInQueue
                    )
                    true
                }
            ),
            contentPadding = PaddingValues(
                top = statusBarHeight,
                bottom = navigationBarHeight
            ),
        ) scope@ {
            val compositionQueue = viewModel.actualQueueState.value
            itemsIndexed(
                items = compositionQueue.tracks,
                key = { _, item ->
                    QueueItemPositionKey(
                        run { check(item.queueID == compositionQueue.queueID) ; item.queueID },
                        item.itemID
                    )
                }
            ) { _, item ->
                TestTaskItemLayout(item)
            }
            // Should this return the recomposition info?
        }
    }

    DisposableEffect(key1 = viewModel, effect = {
        val obs = viewModel.observeQueue()
        onDispose { obs.cancel() }
    })
}

@Composable
private fun ReorderableLazyItemScope.TestTaskItemLayout(
    item: TrackQueueItem
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .reorderingItemVisualModifiers()
            .background(Theme.backgroundColorAsState().value)
    ) {
        Row(
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth(),
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
                    text = item.itemID.toString() + " [${info.indexInMask}] ",
                    color = Theme.backgroundContentColorAsState().value
                )
            }
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .align(Alignment.CenterHorizontally),
            color = Theme.surfaceVariantColorAsState().value
        )
    }
}

internal data class QueueItemPositionLayoutType(
    val unit: Unit = Unit
)


internal data class QueueItemPositionKey(
    val qID: String,
    val idInQueue: Int,
) : Parcelable {
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(qID)
        dest.writeInt(idInQueue)
    }

    companion object {

        @JvmField
        @Suppress("unused")
        val CREATOR = object : Parcelable.Creator<QueueItemPositionKey> {

            override fun createFromParcel(source: Parcel): QueueItemPositionKey {
                return QueueItemPositionKey(
                    source.readString()!!,
                    source.readInt(),
                )
            }

            override fun newArray(size: Int): Array<QueueItemPositionKey?> {
                return arrayOfNulls(size)
            }
        }
    }
}