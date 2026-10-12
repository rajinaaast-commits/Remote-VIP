package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.TvDashboardScreen
import com.example.ui.theme.AuraDarkBg
import com.example.ui.theme.AuraRemoteTheme
import com.example.volume.TvVolumeController

class MainActivity : ComponentActivity() {

    private lateinit var volumeController: TvVolumeController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        volumeController = TvVolumeController(this)

        setContent {
            AuraRemoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AuraDarkBg
                ) {
                    TvDashboardScreen(volumeController = volumeController)
                }
            }
        }
    }
}
