package dev.flammky.compose_components.presentation.reordering

import android.content.Context
import dev.flammky.compose_components.core.LazyConstructor
import dev.flammky.compose_components.core.LazyConstructor.Companion.valueOrNull
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow

interface TaskRepository {

    suspend fun observeTaskList(): Flow<TaskList>

    suspend fun moveTask(
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
            lazyValue = { RealTaskRepository(ctx) },
            lazyThrow = { error("TaskRepository was already provided") }
        )
    }
}



private class RealTaskRepository(
    private val ctx: Context
) : TaskRepository {

    override suspend fun observeTaskList(): Flow<TaskList> {
        TODO("Not yet implemented")
    }

    override suspend fun moveTask(
        snapshotListID: String,
        from: Int,
        to: Int
    ): Deferred<TaskRepository.MutateTaskResult> {
        TODO("Not yet implemented")
    }
}