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
    private val _overrideTaskListState = mutableStateOf<ModifiedTaskList?>(value = null)

    val taskListState = derivedStateOf {
        _taskListState.value
    }
    val overrideTaskListState = derivedStateOf {
        _overrideTaskListState.value?.current
    }
    val maskedTaskListState: State<TaskList> = derivedStateOf {
        overrideTaskListState.value ?: taskListState.value
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

    fun moveTask(
        snapshotListID: String,
        from: Int,
        to: Int,
    ) {
        viewModelScope.launch {

            if (from == to || _taskListState.value.listSnapshotID != snapshotListID) {
                return@launch
            }

            // modification is in the same thread anyways

            val base = _taskListState.value

            if (from !in base.list.indices || to !in base.list.indices) {
                return@launch
            }

            _overrideTaskListState.value = ModifiedTaskList(
                base = base,
                current = base.copy(
                    list = base.list
                        .toMutableList()
                        .apply {
                            add(to, removeAt(from))
                        }
                        .toPersistentList()
                )
            )
        }
    }

    @Immutable
    private data class ModifiedTaskList(
        val base: TaskList,
        val current: TaskList
    )
}

internal class ReorderingStateViewModel() : ViewModel() {

}