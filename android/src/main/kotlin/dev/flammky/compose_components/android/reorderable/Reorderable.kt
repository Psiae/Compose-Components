package dev.flammky.compose_components.android.reorderable

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId

data class ReorderDragStart(
    val id: PointerId,
    val slop: Offset,
    val selfIndex: Int,
    val selfKey: Any
)