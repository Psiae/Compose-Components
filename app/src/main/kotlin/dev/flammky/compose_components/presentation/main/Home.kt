package dev.flammky.compose_components.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun Home(
    hostController: NavHostController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                Button(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = {
                        hostController.navigate("ordering")
                    }
                ) {
                    Text(text = "Ordering Test")
                }
            }
        }
    }
}