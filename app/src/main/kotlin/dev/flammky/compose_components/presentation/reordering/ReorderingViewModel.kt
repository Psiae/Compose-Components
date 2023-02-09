package dev.flammky.compose_components.presentation.reordering

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class ReorderingViewModel : ViewModel() {
    private val _controller = PlaybackController.get()

    private var _observeJobCount = 0
    private var _observeJob: Job? = null

    private val _queueState = mutableStateOf<TrackQueue>(value = TrackQueue.UNSET)
    private val _reorderedQueueState = mutableStateOf<ReorderedQueue?>(value = null)

    val actualQueueState = derivedStateOf {
        _queueState.value
    }
    val reorderedQueueState = derivedStateOf {
        _reorderedQueueState.value?.modified
    }
    val maskedQueueState = derivedStateOf {
        reorderedQueueState.value ?: actualQueueState.value
    }

    fun observeQueue(): Job = viewModelScope.launch {
        if (_observeJob == null) {
            _observeJob = viewModelScope.launch {
                _controller.observeTrackQueue()
                    .collect {
                        _queueState.value = it
                    }
            }
        }
        runCatching {
            _observeJobCount++
            _observeJob!!.join()
        }.onFailure { ex ->
            if (ex !is CancellationException) throw ex
            if (--_observeJobCount == 0) _observeJob!!.cancel()
        }
    }

    fun startMoveTrack(
        qId: String,
        from: Int,
        expectFromID: Int
    ) {
        // should we assert that `overrideTaskListState` should be null ?

        val base = _queueState.value

        if (base.queueID != qId || base.tracks.getOrNull(from)?.itemID != expectFromID) {
            return
        }

        _reorderedQueueState.value = ReorderedQueue(
            base = base,
            modified = base.copy(),
            node = from to from
        )
    }

    fun moveTask(
        qId: String,
        from: Int,
        expectFromID: Int,
        to: Int,
        expectToID: Int
    ) {
        val reordered = _reorderedQueueState.value
            ?: error("")

        val reorderedMod = reordered.modified

        if (reorderedMod.tracks.getOrNull(from)?.itemID != expectFromID) error("")

        val mod = reorderedMod.copy(
            tracks = reorderedMod.tracks
                .toMutableList()
                .apply {
                    add(to, removeAt(from))
                }
                .toPersistentList()
        )

        _reorderedQueueState.value = ReorderedQueue(
            base = reordered.base,
            modified = mod,
            node = (reordered.node.first) to to
        )
    }

    fun cancelMoveTask() {
        viewModelScope.launch {
            _reorderedQueueState.value = null
        }
    }

    fun commitMoveQueueItem() {
        viewModelScope.launch {
            val mod = _reorderedQueueState.value
                ?: return@launch

            _controller
                .moveItemAsync(
                    expectQueueID = mod.modified.queueID,
                    expectTracksMod = mod.modified.tracksMod,
                    expectFromIndex = mod.node.first,
                    expectFromId = mod.base.tracks[mod.node.first].itemID,
                    expectToIndex = mod.node.second,
                    expectToId = mod.base.tracks[mod.node.second].itemID
                )
                .await()
                .notify?.join()

            if (_reorderedQueueState.value === mod) {
                _reorderedQueueState.value = null
            }
        }
    }

    @Immutable
    private data class ReorderedQueue(
        val base: TrackQueue,
        val modified: TrackQueue,
        val node: Pair<Int, Int>
    )
}