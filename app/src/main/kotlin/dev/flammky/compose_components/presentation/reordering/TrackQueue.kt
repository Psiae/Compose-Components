package dev.flammky.compose_components.presentation.reordering

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class TrackQueue(
    val queueID: String,
    //  unified modification number
    val tracksMod: Int,
    val tracks: ImmutableList<TrackQueueItem>,
    val currentTrackItem: TrackQueueItem?,
    val currentTrackItemIndex: Int
) {

    companion object {
        val UNSET = TrackQueue(
            queueID = "",
            tracksMod = 0,
            tracks = persistentListOf(),
            currentTrackItem = null,
            currentTrackItemIndex = -1
        )
    }
}

data class TrackQueueItem(
    val queueID: String,
    val itemID: Int,
    val trackID: String,
)