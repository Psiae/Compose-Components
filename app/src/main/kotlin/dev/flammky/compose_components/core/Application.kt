package dev.flammky.compose_components.core

import android.app.Application
import dev.flammky.compose_components.presentation.reordering.PlaybackController

class CoreApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PlaybackController.provide()
    }
}