package dev.flammky.compose_components.android.reorderable

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId

data class DragStart(
    val id: PointerId,
    val offset: Offset? = null
)