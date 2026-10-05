/*
* Copyright 2026 Michael Moessner
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

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import de.moekadu.tuner.R
import de.moekadu.tuner.notedetection.SoundSourceError

/** User-facing description of a sound source failure. */
@Composable
fun soundSourceErrorMessage(error: SoundSourceError): String = when (error) {
    is SoundSourceError.InvalidBufferSize ->
        stringResource(R.string.sound_source_invalid_buffer_size, error.code)

    SoundSourceError.MicrophoneUnavailable ->
        stringResource(R.string.sound_source_microphone_unavailable)

    is SoundSourceError.ReadFailed ->
        stringResource(R.string.sound_source_read_failed, error.code)
}

/** Show a snackbar while the sound source is failing.
 * The snackbar is dismissed as soon as the error is reset to null (e.g. on tuner restart).
 * @param error Current sound source error or null.
 * @param snackbarHostState Where to show the snackbar.
 */
@Composable
fun SoundSourceErrorSnackbar(error: SoundSourceError?, snackbarHostState: SnackbarHostState) {
    val message = error?.let { soundSourceErrorMessage(it) }
    LaunchedEffect(message, snackbarHostState) {
        if (message != null) {
            snackbarHostState.showSnackbar(
                message,
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite
            )
        }
    }
}
