package dev.flammky.compose_components.core

import androidx.compose.ui.Modifier

internal inline fun Modifier.combineIf(
    predicate: Boolean,
    combine: Modifier.() -> Modifier
): Modifier = if (predicate) this.combine() else this