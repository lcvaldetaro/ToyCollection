package com.gepetto.toys

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