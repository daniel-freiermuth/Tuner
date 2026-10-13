package de.moekadu.tuner

import de.moekadu.tuner.notedetection.AcousticZeroWeighting
import de.moekadu.tuner.notedetection.FrequencyDetectionResultCollector
import de.moekadu.tuner.notedetection.MemoryPoolSampleData
import de.moekadu.tuner.notedetection.WindowingFunction
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FrequencyDetectionResultCollectorTest {
    /** Plucked-string like tone: weak fundamental, dominant second harmonic, decaying amplitude.
     * Once the attack is over, every frame is quieter than and similar to the previous one, so the
     * collector takes its "autocorrelation stays similar" shortcut; it must keep the fundamental
     * instead of jumping to the second harmonic.
     */
    @Test
    fun decayingToneWithWeakFundamentalKeepsFundamental() = runBlocking {
        val sampleRate = 44100
        val windowSize = 4096
        val hopSize = 1024
        val numFrames = 40
        val onset = 4 * hopSize
        val amplitudes = floatArrayOf(0.2f, 1f, 0.25f, 0.4f, 0.15f, 0.2f)
        val decayTime = 1.5f

        for (baseFrequency in floatArrayOf(82.41f, 110f, 146.83f, 196f)) {
            val signal = FloatArray(windowSize + numFrames * hopSize) { i ->
                if (i < onset) {
                    0f
                } else {
                    val t = (i - onset).toFloat() / sampleRate
                    var value = 0f
                    for (k in amplitudes.indices) {
                        value += amplitudes[k] *
                            sin(2f * PI.toFloat() * (k + 1) * baseFrequency * t)
                    }
                    0.3f * exp(-t / decayTime) * value
                }
            }

            // same settings as used in Tuner.kt
            val collector = FrequencyDetectionResultCollector(
                frequencyMin = 16f,
                frequencyMax = 16000f,
                subharmonicsTolerance = 0.1f,
                subharmonicsPeakRatio = 0.75f,
                harmonicTolerance = 0.11f,
                minimumFactorOverLocalMean = 3f,
                maxGapBetweenHarmonics = 5,
                maxNumHarmonicsForInharmonicity = 8,
                windowType = WindowingFunction.Tophat,
                acousticWeighting = AcousticZeroWeighting()
            )
            val sampleDataPool = MemoryPoolSampleData(4)

            for (frame in 0 until numFrames) {
                val framePosition = frame * hopSize
                val sampleData = sampleDataPool.get(windowSize, sampleRate, framePosition)
                sampleData.memory.addData(
                    framePosition,
                    signal.copyOfRange(framePosition, framePosition + windowSize)
                )
                val result = collector.collectResults(sampleData)
                sampleData.decRef()
                // only check frames, which lie completely inside the tone
                if (framePosition >= onset) {
                    assertEquals(
                        "base frequency $baseFrequency, frame $frame",
                        baseFrequency,
                        result.memory.frequency,
                        0.02f * baseFrequency
                    )
                }
                result.decRef()
            }
        }
    }
}
