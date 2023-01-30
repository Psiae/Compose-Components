package dev.flammky.compose_components.presentation.reordering

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class ReorderingViewModel : ViewModel() {
    private val _repo = TaskRepository.get()

    private var _taskJobCount = 0
    private var _taskJob: Job? = null

    private val _taskListState = mutableStateOf<TaskList>(value = TaskList.UNSET)
    private val _overrideTaskListState = mutableStateOf<ReorderedTaskList?>(value = null)

    val actualTaskListState = derivedStateOf {
        _taskListState.value
    }
    val overrideTaskListState = derivedStateOf {
        _overrideTaskListState.value?.modified
    }
    val maskedTaskListState: State<TaskList> = derivedStateOf {
        overrideTaskListState.value ?: actualTaskListState.value
    }

    fun observeTask(): Job = viewModelScope.launch {
        if (_taskJob == null) {
            _taskJob = viewModelScope.launch {
                _repo.observeTaskList()
                    .collect {
                        _taskListState.value = it
                    }
            }
        }
        runCatching {
            _taskJobCount++
            _taskJob!!.join()
        }.onFailure { ex ->
            if (ex !is CancellationException) throw ex
            if (--_taskJobCount == 0) _taskJob!!.cancel()
        }
    }

    fun startMoveTask(
        snapshotListID: String,
        from: Int
    ) {
        viewModelScope.launch {

            // should we assert that `overrideTaskListState` should be null ?

            val base = _taskListState.value

            if (base.listSnapshotID != snapshotListID) {
                return@launch
            }

            if (from !in base.list.indices) {
                return@launch
            }

            _overrideTaskListState.value = ReorderedTaskList(
                base = base,
                modified = base.copy(),
                node = from to from
            )
        }
    }

    fun moveTask(
        snapshotListID: String,
        from: Int,
        to: Int,
    ) {
        viewModelScope.launch {

            val reordered = _overrideTaskListState.value
                ?: return@launch

            val reorderedMod = reordered.modified

            if (from == to || reordered.modified.listSnapshotID != snapshotListID) {
                return@launch
            }

            if (from !in reorderedMod.list.indices || to !in reorderedMod.list.indices) {
                return@launch
            }

            val mod = reorderedMod.copy(
                list = reorderedMod.list
                    .toMutableList()
                    .apply {
                        add(to, removeAt(from))
                    }
                    .toPersistentList()
            )

            _overrideTaskListState.value = ReorderedTaskList(
                base = reordered.base,
                modified = mod,
                node = (reordered.node.first) to to
            )
        }
    }

    fun cancelMoveTask() {
        viewModelScope.launch {
            _overrideTaskListState.value = null
        }
    }

    fun commitMoveTask() {
        viewModelScope.launch {
            val mod = _overrideTaskListState.value
                ?: return@launch

            if (mod.modified.listSnapshotID != actualTaskListState.value.listSnapshotID) {
                // the List snapshot was changed
                _overrideTaskListState.value = null
                return@launch
            }

            _repo.moveTask(mod.modified.listSnapshotID, mod.node.first, mod.node.second)
                .await()
                .notifyTask?.join()

            if (_overrideTaskListState.value === mod) {
                _overrideTaskListState.value = null
            }
        }
    }

    @Immutable
    private data class ReorderedTaskList(
        val base: TaskList,
        val modified: TaskList,
        val node: Pair<Int, Int>
    )
}