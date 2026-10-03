package com.typdevstudio.typos_movil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.typdevstudio.typos_movil.ui.navegacion.NavegacionApp
import com.typdevstudio.typos_movil.ui.theme.TyPOS_MovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TyPOS_MovilTheme {
                NavegacionApp()
            }
        }
    }
}