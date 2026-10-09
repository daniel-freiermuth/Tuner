/*
 * Copyright 2020 Michael Moessner
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

package de.moekadu.tuner.notedetection

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.misc.WaveWriter
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Sound source job.
 * @param channel Channel delivering sample windows. If the sound source fails, the channel is
 *   closed with a [SoundSourceException], which is rethrown when receiving from the channel.
 */
class SoundSourceJob(val channel: ReceiveChannel<MemoryPool<SampleData>.RefCountedMemory>)

/** Reason why the sound source cannot deliver samples. */
sealed class SoundSourceError {
    /** AudioRecord.getMinBufferSize returned the error [code] instead of a buffer size. */
    data class InvalidBufferSize(val code: Int) : SoundSourceError()

    /** The microphone could not be acquired, e.g. because another app is using it. */
    data object MicrophoneUnavailable : SoundSourceError()

    /** AudioRecord.read returned the error [code], e.g. AudioRecord.ERROR_DEAD_OBJECT. */
    data class ReadFailed(val code: Int) : SoundSourceError()
}

/** Close cause of [SoundSourceJob.channel] if the sound source fails. */
class SoundSourceException(val error: SoundSourceError, cause: Throwable? = null) :
    Exception("Sound source failed: $error", cause)

private const val LOG_TAG = "Tuner"

/** Generator of sound samples.
 * Fails loudly: if the microphone cannot be acquired or reading from it fails, the returned
 * channel is closed with a [SoundSourceException].
 */
fun CoroutineScope.launchSoundSourceJob(
    overlap: Float = 0.25f,
    windowSize: Int = 4096,
    sampleRate: Int = 44100,
    testFunction: ((frame: Int, dt: Float) -> Float)? = null,
    waveWriter: WaveWriter? = null
): SoundSourceJob {
    val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )
    if (minBufferSize <= 0) {
        val exception = SoundSourceException(SoundSourceError.InvalidBufferSize(minBufferSize))
        Log.e(LOG_TAG, "SoundSource: invalid buffer size for sample rate $sampleRate", exception)
        val failedChannel = Channel<MemoryPool<SampleData>.RefCountedMemory>()
        failedChannel.close(exception)
        return SoundSourceJob(failedChannel)
    }

    val channelCapacity = computeRequiredChannelCapacity(
        audioRecordBufferSizeInFrames = minBufferSize / Short.SIZE_BYTES,
        windowSize = windowSize,
        overlap = overlap
    )

    val outputChannel = Channel<MemoryPool<SampleData>.RefCountedMemory>(
        channelCapacity,
        BufferOverflow.SUSPEND
    )
    val memoryPool =
        // trial shows that single channel capacity does not very well recycle data in extreme
        // cases, double channel capacity seems to work fine
        MemoryPoolSampleData(2 * channelCapacity)

    launch(Dispatchers.IO) {
        var record: AudioRecord? = null
        try {
            val recordData: ShortArray
            val readFrames: suspend (ShortArray) -> Int
            if (testFunction == null) {
                val audioRecord = acquireAudioRecord(sampleRate, minBufferSize)
                record = audioRecord
                audioRecord.startRecording()
                recordData = ShortArray(audioRecord.bufferSizeInFrames / 2)
                readFrames = { buffer ->
                    audioRecord.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                }
            } else {
                recordData = ShortArray(minBufferSize / Short.SIZE_BYTES / 2)
                var testFrame = 0
                readFrames = { buffer ->
                    for (i in buffer.indices) {
                        buffer[i] = (
                            Short.MAX_VALUE * testFunction(testFrame + i, 1f / sampleRate)
                            ).toInt().toShort()
                    }
                    delay((1000 * buffer.size.toFloat() / sampleRate).toLong())
                    testFrame += buffer.size
                    buffer.size
                }
            }
            produceSampleData(
                readFrames = readFrames,
                recordData = recordData,
                outputChannel = outputChannel,
                memoryPool = memoryPool,
                windowSize = windowSize,
                sampleRate = sampleRate,
                overlap = overlap,
                waveWriter = waveWriter
            )
        } catch (e: SoundSourceException) {
            Log.e(LOG_TAG, "SoundSource: stopped recording", e)
            outputChannel.close(e)
        } finally {
            record?.stop()
            record?.release()
            // no-op if already closed with an error
            outputChannel.close()
        }
    }
    return SoundSourceJob(outputChannel)
}

/** Create an initialized AudioRecord for the microphone.
 * @throws SoundSourceException with [SoundSourceError.MicrophoneUnavailable] if the
 *   microphone cannot be acquired.
 */
@SuppressLint("MissingPermission")
private fun acquireAudioRecord(sampleRate: Int, bufferSizeInBytes: Int): AudioRecord {
    val record = try {
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes
        )
    } catch (e: IllegalArgumentException) {
        throw SoundSourceException(SoundSourceError.MicrophoneUnavailable, e)
    }
    if (record.state != AudioRecord.STATE_INITIALIZED) {
        record.release()
        throw SoundSourceException(SoundSourceError.MicrophoneUnavailable)
    }
    return record
}

/** Read frames until the coroutine is cancelled and send full sample windows to [outputChannel].
 * @param readFrames Fills the given buffer and returns the number of frames read, or a negative
 *   AudioRecord error code.
 * @throws SoundSourceException with [SoundSourceError.ReadFailed] if [readFrames] returns an
 *   error code. Such errors are terminal, e.g. ERROR_DEAD_OBJECT returns immediately on every
 *   further read.
 */
internal suspend fun produceSampleData(
    readFrames: suspend (ShortArray) -> Int,
    recordData: ShortArray,
    outputChannel: SendChannel<MemoryPool<SampleData>.RefCountedMemory>,
    memoryPool: MemoryPoolSampleData,
    windowSize: Int,
    sampleRate: Int,
    overlap: Float,
    waveWriter: WaveWriter?
) {
    val sampleDataList = ArrayList<MemoryPool<SampleData>.RefCountedMemory>()
    var nextStartingDataFrame = 0
    var currentFrame = 0
    while (currentCoroutineContext().isActive) {
        val numRead = readFrames(recordData)
        if (numRead < 0) {
            throw SoundSourceException(SoundSourceError.ReadFailed(numRead))
        }

        // Log.v("TestRecordFlow", "SoundSource: numRead=$numRead, currentFrame=$currentFrame, windowSize=$windowSize")
        if (numRead > 0) {
            // add empty sampleData objects to the data queue
            while (nextStartingDataFrame <= currentFrame + numRead) {
                // sampleDataList.add(SampleData(windowSize, sampleRate, nextStartingDataFrame))
                sampleDataList.add(
                    memoryPool.get(
                        windowSize,
                        sampleRate,
                        nextStartingDataFrame
                    )
                )
                nextStartingDataFrame += max(
                    1,
                    ((1.0 - overlap) * windowSize).roundToInt()
                )
            }
            // Log.v("TestRecordFlow", "SoundSource: sampleDataList.size = ${sampleDataList.size}, nextStartingDataFrame=$nextStartingDataFrame")
            sampleDataList
                .map { it.apply { memory.addData(currentFrame, recordData) } }
                .filter { it.memory.isFull }
                .map {
//                                Log.v("Tuner", "SoundSource: sending sample data at frame: ${it.memory.framePosition}")
                    // outputChannel.send(it)
                    val sendStatus = outputChannel.trySend(it)
                    if (!sendStatus.isSuccess) {
                        it.decRef()
                    }
                }
            sampleDataList.removeAll { it.memory.isFull }

            // for(s in sampleDataList)
            //    Log.v("TestRecordFlow", "is full: ${s.isFull}")

            currentFrame += numRead

            waveWriter?.appendData(recordData, numRead)
        }
    }
}

private fun computeRequiredChannelCapacity(
    audioRecordBufferSizeInFrames: Int,
    windowSize: Int,
    overlap: Float
): Int {
    val updateRateInFrames = max(1, 1 + (windowSize * (1.0 - overlap)).toInt())
//        Log.v("Tuner", "SoundSource: windowSize = $windowSize, overlap = $overlap, updateRateInFrames = $updateRateInFrames" )
    // multiply by 2, to allow writing to the output channel within one iteration
    // without loosing data and additionally have enough capacity to store a second cycle
    return max(2, 2 * audioRecordBufferSizeInFrames / updateRateInFrames)
}
