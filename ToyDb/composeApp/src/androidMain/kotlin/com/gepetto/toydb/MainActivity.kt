package com.gepetto.toydb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import club.gepetto.composeutils.GcTheme
import com.gepetto.toydb.database.createDatabase
import com.gepetto.toydb.ui.ToyDbNavigation
import com.gepetto.toydb.utils.AndroidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Android context for ImageResolver and Database
        AndroidContext.appContext = applicationContext
        val db = createDatabase(this, "toydb.db")

        enableEdgeToEdge()

        setContent {
            GcTheme {
                ToyDbNavigation(db)
            }
        }
    }
}
