package com.gepetto.toys

import android.os.Looper
import club.gepetto.circum.CircumIntentProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Thread.sleep

class AppIntentProcessor : CircumIntentProcessor<AppState, AppIntentCommand, AppEffect>() {

    init {
        setState(AppState.Loading)
        installGameFiles()
    }

    private fun installGameFiles() {
        CoroutineScope(Dispatchers.IO).launch {
            Looper.prepare() // needed to issue toasts
            setState(AppState.Loaded)
        }
    }
}

sealed interface AppState {
   data object Loading : AppState
   data object Loaded : AppState
}

data object AppIntentCommand
data object AppEffect