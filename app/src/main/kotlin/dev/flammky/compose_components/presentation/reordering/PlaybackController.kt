package dev.flammky.compose_components.presentation.reordering

import android.util.Log
import dev.flammky.compose_components.core.LazyConstructor
import dev.flammky.compose_components.core.LazyConstructor.Companion.valueOrNull
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

interface PlaybackController {
    fun observeTrackQueue(): Flow<TrackQueue>

    fun getCachedTrackQueueAsync(): Deferred<TrackQueue>

    fun moveItemAsync(
        // Instead of these we should compare the `path` of the reorder
        expectQueueID: String,
        expectTracksMod: Int,
        expectFromIndex: Int,
        expectFromId: Int,
        expectToIndex: Int,
        expectToId: Int,
    ): Deferred<MutateQueueResult>


    data class MutateQueueResult(
        val success: Boolean,
        val notify: Job?
    )


    companion object {
        private val SINGLETON = LazyConstructor<PlaybackController>()

        fun get(): PlaybackController = SINGLETON.valueOrNull()
            ?: error("PlaybackController was not provided")

        fun provide(): PlaybackController = SINGLETON.constructOrThrow(
            lazyValue = { RealPlaybackController() },
            lazyThrow = { error("PlaybackController was already provided") }
        )
    }
}

private class RealPlaybackController() : PlaybackController {

    private val _dispatcher = Dispatchers.Default.limitedParallelism(1)
    private val _supervisorScope = CoroutineScope(SupervisorJob())
    private val _taskListSharedFlow = MutableSharedFlow<TrackQueue>(1)
    private var _mod: Int = 0
    private var _qMod: Int = 0
    private var _queue: TrackQueue

    init {
        _queue = run {
            val qID = "gen1"
            val tracks = mutableListOf<TrackQueueItem>()
                .apply {
                    for (i in 1..100) {
                        add(TrackQueueItem(qID, i, (100 + i).toString()))
                    }
                }
                .toPersistentList()
            val i = Random.nextInt(100)
            TrackQueue(
                queueID = qID,
                tracksMod = 1,
                tracks = tracks,
                currentTrackItem = tracks[i],
                currentTrackItemIndex = i
            )
        }
        check(_taskListSharedFlow.tryEmit(_queue))
    }

    override fun observeTrackQueue(): Flow<TrackQueue> {
        return flow { _taskListSharedFlow.collect(this) }
    }

    override fun getCachedTrackQueueAsync(): Deferred<TrackQueue> {
        return _supervisorScope.async(_dispatcher) { _queue }
    }

    override fun moveItemAsync(
        expectQueueID: String,
        expectTracksMod: Int,
        expectFromIndex: Int,
        expectFromId: Int,
        expectToIndex: Int,
        expectToId: Int,
    ): Deferred<PlaybackController.MutateQueueResult> {
        Log.d("PlaybackController", "moveItemAsync($expectQueueID, $expectTracksMod, $expectFromIndex, $expectFromId, $expectToIndex, $expectToId)")
        return _supervisorScope.async(_dispatcher) {
            if (_queue.queueID != expectQueueID ||
                _queue.tracksMod != expectTracksMod ||
                _queue.tracks.lastIndex < expectToIndex ||
                _queue.tracks.getOrNull(expectFromIndex)?.itemID != expectFromId ||
                _queue.tracks.getOrNull(expectToIndex)?.itemID != expectToId
            ) {
                return@async PlaybackController.MutateQueueResult(
                    success = false,
                    notify = null
                )
            }
            val ci = when (_queue.currentTrackItem?.itemID) {
                expectFromId -> expectToIndex
                expectToId -> expectFromIndex
                else -> _queue.currentTrackItemIndex
            }
            val c = _queue.tracks[ci]
            val mod = ++_mod
            val tsMod = ++_qMod
            val new = TrackQueue(
                queueID = expectQueueID,
                tracks = _queue.tracks.toMutableList()
                    .apply {
                        add(expectToIndex, removeAt(expectFromIndex))
                    }
                    .toPersistentList(),
                currentTrackItem = c,
                currentTrackItemIndex = ci,
                tracksMod = tsMod
            )
            _queue = new
            PlaybackController.MutateQueueResult(
                success = true,
                notify = _supervisorScope.launch(_dispatcher) {
                    if (_mod == mod) _taskListSharedFlow.emit(new)
                }
            )
        }
    }
}