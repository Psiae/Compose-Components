package dev.flammky.compose_components.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import dev.flammky.compose_components.presentation.main.setContent

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent()
    }
}