package dev.flammky.compose_components.core

import android.app.Application
import dev.flammky.compose_components.presentation.reordering.TaskRepository

class CoreApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        TaskRepository.provide(this)
    }
}