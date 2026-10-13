package de.moekadu.tuner.ui

import de.moekadu.tuner.ui.tuning.PitchHistoryState
import kotlin.math.pow
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchHistoryStateTest {
    private val sampleRate = 44100

    @Test
    fun pitchHistorySizeIsPositiveOverWholePreferenceRange() {
        // Ranges as offered by the preference sliders: window size exponent 7..15,
        // overlap 0..80 % in steps of 5 %, duration 0.25..10 s in steps of 0.25 s.
        for (exponent in 7..15) {
            val windowSize = 2f.pow(exponent).roundToInt()
            for (overlapPercent in 0..80 step 5) {
                for (durationQuarters in 1..40) {
                    val duration = durationQuarters * 0.25f
                    val size = PitchHistoryState.computePitchHistorySize(
                        duration,
                        sampleRate,
                        windowSize,
                        overlapPercent / 100f
                    )
                    assertTrue(
                        "size=$size for duration=$duration, windowSize=$windowSize, overlap=$overlapPercent%",
                        size >= 1
                    )
                }
            }
        }
    }

    @Test
    fun addFrequencyWithSmallestPitchHistoryKeepsOneValue() {
        val size = PitchHistoryState.computePitchHistorySize(0.25f, sampleRate, 32768, 0f)
        val state = PitchHistoryState(size)
        state.addFrequency(440f)
        state.addFrequency(441f)
        assertEquals(1, state.lineCoordinates.size)
        assertEquals(441f, state.lineCoordinates.coordinates[0].y)
    }
}
