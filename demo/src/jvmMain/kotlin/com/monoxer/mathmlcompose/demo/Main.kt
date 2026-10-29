package com.monoxer.mathmlcompose.demo

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MathML Compose Demo",
            state = WindowState(width = 1080.dp, height = 820.dp),
        ) {
            DemoApp()
        }
    }
