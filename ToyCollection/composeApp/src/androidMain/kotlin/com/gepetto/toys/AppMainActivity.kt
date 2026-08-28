package com.gepetto.toys

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import club.gepetto.circum.circumIntentProcessor
import club.gepetto.composeutils.GcE2eBox
import club.gepetto.composeutils.GcTheme
import androidx.compose.runtime.getValue
import org.koin.compose.KoinContext
import com.gepetto.toycollection.ui.ToyCollectionNavigation

class AppMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            GcTheme {
                KoinContext {
                    val aIp = circumIntentProcessor<AppIntentProcessor>()
                    val state by aIp.collectState(AppState.Loading)

                    when (state) {
                        is AppState.Loading -> ToyAppLoading()
                        is AppState.Loaded -> GcE2eBox {
                            ToyCollectionNavigation()
                        }
                    }
                }
            }
        }
    }
}

