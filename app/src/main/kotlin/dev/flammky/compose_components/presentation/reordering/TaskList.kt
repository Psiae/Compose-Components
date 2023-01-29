package dev.flammky.compose_components.presentation.reordering

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class TaskList(
    val snapshotID: String,
    val listSnapshotID: String,
    val list: ImmutableList<Item>,
    val currentTaskIndex: Int,
    val currentTaskId: Int
) {

    data class Item(
        val id: String
    ) {

    }

    companion object {
        val UNSET = TaskList(
            "",
            "",
            persistentListOf(),
            -1,
            -1
        )
    }
}