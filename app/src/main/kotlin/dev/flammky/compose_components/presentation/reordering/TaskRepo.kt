package dev.flammky.compose_components.presentation.reordering

import android.content.Context
import android.os.SystemClock
import android.util.Log
import dev.flammky.compose_components.core.LazyConstructor
import dev.flammky.compose_components.core.LazyConstructor.Companion.valueOrNull
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

interface TaskRepository {

    fun observeTaskList(): Flow<TaskList>

    fun moveTask(
        snapshotListID: String,
        from: Int,
        to: Int,
    ): Deferred<MutateTaskResult>

    data class MutateTaskResult(
        val success: Boolean,
        val notifyTask: Job?
    )

    companion object {
        private val INSTANCE = LazyConstructor<TaskRepository>()

        fun get() = INSTANCE.valueOrNull()
            ?: error("TaskRepository was not provided")

        fun provide(ctx: Context): TaskRepository = INSTANCE.constructOrThrow(
            lazyValue = { RealTaskRepository(ctx.applicationContext) },
            lazyThrow = { error("TaskRepository was already provided") }
        )
    }
}



private class RealTaskRepository(
    private val ctx: Context
) : TaskRepository {

    private val _mutateDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val _supervisorScope = CoroutineScope(SupervisorJob())
    private val _taskListSharedFlow = MutableSharedFlow<TaskList>(1)
    private var _taskList: TaskList

    init {
        val list = mutableListOf<TaskList.Item>()
            .apply {
                for (i in 1..100) {
                    add(
                        TaskList.Item(i.toString())
                    )
                }
            }
        _taskList = newTask(list.toPersistentList(), Random.nextInt(list.lastIndex))
        check(_taskListSharedFlow.tryEmit(_taskList))
    }

    override fun observeTaskList(): Flow<TaskList> {
        return flow { _taskListSharedFlow.collect(this) }
    }

    override fun moveTask(
        snapshotListID: String,
        from: Int,
        to: Int
    ): Deferred<TaskRepository.MutateTaskResult> {
        Log.d("TaskRepo", "moveTask($snapshotListID, $from, $to)")
        return _supervisorScope.async(_mutateDispatcher) {
            // random
            val randomFail = Random.nextInt(11) == 10

            if (randomFail || from == to || _taskList.listSnapshotID != snapshotListID) {
                return@async TaskRepository.MutateTaskResult(false, null)
            }

            if (from !in _taskList.list.indices || to !in _taskList.list.indices) {
                return@async TaskRepository.MutateTaskResult(false, null)
            }

            val newList = (ArrayList(_taskList.list))
                .apply {
                    add(to, removeAt(from))
                }

            val new = newTask(
                newList.toPersistentList(),
                if (_taskList.currentTaskIndex == from) to else _taskList.currentTaskIndex
            )

            _taskList = new

            val notify = launch {
                _taskListSharedFlow.emit(new)
            }

            TaskRepository.MutateTaskResult(true, notify)
        }
    }

    private fun newTask(
        list: ImmutableList<TaskList.Item>,
        currentTaskIndex: Int
    ): TaskList {
        val stamp = System.nanoTime() + SystemClock.elapsedRealtimeNanos()
        val c = C.incrementAndGet()
        val cL = C_List.incrementAndGet()
        val lH = list.sumOf { it.hashCode() % (it.hashCode() + 100) }

        val newSnapshotListID: String =
            "$stamp-$cL-$lH"

        val newSnapshotID: String =
            "$stamp-$c-$lH-${list[currentTaskIndex].id}-${currentTaskIndex}"

        return TaskList(
            snapshotID = newSnapshotID,
            listSnapshotID = newSnapshotListID,
            list = list,
            currentTask = list[currentTaskIndex],
            currentTaskIndex = currentTaskIndex,
        )
    }

    companion object {
        private val C = AtomicLong(0)
        private val C_List = AtomicLong(0)
    }
}