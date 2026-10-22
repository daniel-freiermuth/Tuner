/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.ui.misc

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import de.moekadu.tuner.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

private const val AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO

/** Snapshot of the RECORD_AUDIO permission; equality drives effect restarts. */
private data class AudioPermissionStatus(val isGranted: Boolean, val shouldShowRationale: Boolean)

private fun readAudioPermissionStatus(
    context: Context,
    activity: Activity?
): AudioPermissionStatus {
    val granted = ContextCompat.checkSelfPermission(context, AUDIO_PERMISSION) ==
        PackageManager.PERMISSION_GRANTED
    return AudioPermissionStatus(
        isGranted = granted,
        shouldShowRationale = !granted && activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity, AUDIO_PERMISSION)
    )
}

/** Permission status, re-read whenever the lifecycle resumes (e.g. back from system settings). */
@Composable
private fun rememberAudioPermissionStatus(
    context: Context,
    activity: Activity?
): MutableState<AudioPermissionStatus> {
    val status = remember(context, activity) {
        mutableStateOf(readAudioPermissionStatus(context, activity))
    }
    LifecycleResumeEffect(status, context, activity) {
        status.value = readAudioPermissionStatus(context, activity)
        onPauseOrDispose { }
    }
    return status
}

/** Whether RECORD_AUDIO is granted, without ever requesting it. Re-checked on resume. */
@Composable
fun rememberAudioPermissionGranted(): Boolean {
    val status by rememberAudioPermissionStatus(LocalContext.current, LocalActivity.current)
    return status.isGranted
}

private fun CoroutineScope.launchSnackbar(
    context: Context,
    snackbarHostState: SnackbarHostState,
    requestPermission: () -> Unit
) {
    launch {
        val result = snackbarHostState.showSnackbar(
            context.getString(R.string.audio_record_permission_rationale),
            actionLabel = context.getString(R.string.settings),
            withDismissAction = false
        )
        when (result) {
            SnackbarResult.Dismissed -> {}

            SnackbarResult.ActionPerformed -> {
                requestPermission()
            }
        }
    }
}

@Composable
fun rememberTunerAudioPermission(snackbarHostState: SnackbarHostState): Boolean {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val reopenSnackbarChannel = remember { Channel<Boolean>(Channel.CONFLATED) }
    var status by rememberAudioPermissionStatus(context, activity)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        status = readAudioPermissionStatus(context, activity)
        if (!granted) {
            reopenSnackbarChannel.trySend(false)
        }
    }
    val requestPermission = { launcher.launch(AUDIO_PERMISSION) }
//    Log.v("Tuner", "MainGraph: 1: permissions_granted = ${status.isGranted}, rational = ${status.shouldShowRationale}")

    // TODO: relaunching seems to fail ...
    LaunchedEffect(status, reopenSnackbarChannel) {
        if (!status.isGranted) {
            if (status.shouldShowRationale) {
                launchSnackbar(context, snackbarHostState, requestPermission)
            } else {
                requestPermission()
            }

            for (reopen in reopenSnackbarChannel) {
                launchSnackbar(context, snackbarHostState, requestPermission)
            }
        }
    }
    return status.isGranted
}
