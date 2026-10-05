package de.moekadu.tuner

import android.media.AudioRecord
import de.moekadu.tuner.misc.MemoryPool
import de.moekadu.tuner.notedetection.MemoryPoolSampleData
import de.moekadu.tuner.notedetection.SampleData
import de.moekadu.tuner.notedetection.SoundSourceError
import de.moekadu.tuner.notedetection.SoundSourceException
import de.moekadu.tuner.notedetection.produceSampleData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SoundSourceTest {
    @Test
    fun readErrorTerminatesWithTypedError() = runBlocking {
        val outputChannel = Channel<MemoryPool<SampleData>.RefCountedMemory>(Channel.UNLIMITED)
        var numReads = 0

        try {
            // without terminal handling, the dead recorder would be polled forever
            withTimeout(5000) {
                produceSampleData(
                    readFrames = {
                        numReads++
                        AudioRecord.ERROR_DEAD_OBJECT
                    },
                    recordData = ShortArray(512),
                    outputChannel = outputChannel,
                    memoryPool = MemoryPoolSampleData(4),
                    windowSize = 1024,
                    sampleRate = 44100,
                    overlap = 0.25f,
                    waveWriter = null
                )
            }
            fail("Expected SoundSourceException")
        } catch (e: SoundSourceException) {
            assertEquals(SoundSourceError.ReadFailed(AudioRecord.ERROR_DEAD_OBJECT), e.error)
        }

        assertEquals(1, numReads)
        assertTrue(outputChannel.tryReceive().isFailure)
    }
}
