package de.moekadu.tuner

import de.moekadu.tuner.misc.GetTextFromString
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.stretchtuning.StretchTuning
import de.moekadu.tuner.temperaments.centsToFrequency
import de.moekadu.tuner.temperaments.updateActiveCustomStretchTuning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ActiveCustomStretchTuningTest {
    // no stretch at the reference frequency, given stretch one octave above
    private fun customStretchTuning(octaveStretchInCents: Double, stableId: Long) = StretchTuning(
        name = GetTextFromString("custom"),
        description = GetTextFromString(""),
        unstretchedFrequencies = doubleArrayOf(440.0, 880.0),
        stretchInCents = doubleArrayOf(0.0, octaveStretchInCents),
        stableId = stableId
    )

    private val activeStretchTuning = customStretchTuning(0.0, 5L)
    private val scale = MusicalScale2.createTestEdo12().copy(_stretchTuning = activeStretchTuning)

    @Test
    fun editedActiveStretchTuningIsUsedForNoteFrequencies() {
        val edited = customStretchTuning(10.0, activeStretchTuning.stableId)
        val otherStretchTuning = customStretchTuning(20.0, 6L)

        val updated = updateActiveCustomStretchTuning(scale, listOf(otherStretchTuning, edited))

        assertNotNull(updated)
        assertEquals(edited, updated!!.stretchTuning)
        assertEquals(centsToFrequency(10.0, 880.0).toFloat(), updated.getNoteFrequency(12), 1e-2f)
        assertEquals(880f, scale.getNoteFrequency(12), 1e-2f)
        assertEquals(scale.temperament, updated.temperament)
        assertEquals(scale.rootNote, updated.rootNote)
        assertEquals(scale.referenceNote, updated.referenceNote)
        assertEquals(scale.referenceFrequency, updated.referenceFrequency)
    }

    @Test
    fun activeStretchTuningNotInListKeepsScale() {
        val updated = updateActiveCustomStretchTuning(scale, listOf(customStretchTuning(10.0, 6L)))
        assertNull(updated)
    }
}
