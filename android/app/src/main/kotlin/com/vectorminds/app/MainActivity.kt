package com.vectorminds.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.vectorminds.app.ui.navigation.VectorMindsNavGraph
import com.vectorminds.app.ui.theme.VectorMindsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VectorMindsTheme {
                val navController = rememberNavController()
                VectorMindsNavGraph(navController = navController)
            }
        }
    }
}
