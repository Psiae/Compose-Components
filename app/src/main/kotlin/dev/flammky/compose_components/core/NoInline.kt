package dev.flammky.compose_components.core

import androidx.compose.runtime.Composable

@Composable
fun NoInline(block: @Composable () -> Unit) = block()