package com.gepetto.toydb.platform

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred

@Composable
actual fun rememberStoragePermissionRequest(): suspend () -> Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return remember { { true } }
    }
    val context = LocalContext.current
    var pendingDeferred: CompletableDeferred<Boolean>? = remember { null }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        pendingDeferred?.complete(allGranted)
        pendingDeferred = null
    }

    return remember(launcher, context) {
        suspend {
            val writeGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val readGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            if (writeGranted && readGranted) {
                true
            } else {
                val deferred = CompletableDeferred<Boolean>()
                pendingDeferred = deferred
                launcher.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
                deferred.await()
            }
        }
    }
}
