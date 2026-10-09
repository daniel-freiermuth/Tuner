package de.moekadu.tuner

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.notedetection.TargetNoteAutoDetection
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.temperaments.predefinedTemperamentEDO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TargetNoteAutoDetectionTest {
    // reference note A4 = 440Hz has note index 0
    private val scale = MusicalScale2.createTestEdo12()

    private val noteE2 = scale.getNote(-29)
    private val noteA2 = scale.getNote(-24)
    private val noteD3 = scale.getNote(-19)
    private val noteG3 = scale.getNote(-14)
    private val noteB3 = scale.getNote(-10)
    private val noteE4 = scale.getNote(-5)

    /** Note which does not exist in a 12-EDO scale. */
    private val noteNotInScale = MusicalNote(BaseNote.C, NoteModifier.NaturalUp, octave = 4)

    private fun instrument(vararg strings: MusicalNote) = Instrument(
        name = "test",
        nameResource = null,
        strings = arrayOf(*strings),
        icon = InstrumentIcon.entries[0],
        stableId = 1L
    )

    // strings intentionally unsorted and with a duplicate
    private val guitar = instrument(noteE4, noteA2, noteB3, noteE2, noteG3, noteD3, noteA2)

    /** Frequency of a (possibly fractional) note index of the 12-EDO test scale. */
    private fun f(noteIndex: Float) = scale.getNoteFrequency(noteIndex)

    @Test
    fun chromaticReturnsClosestNoteWithoutPreviousNote() {
        val detection = TargetNoteAutoDetection(scale, null, 5f)
        assertEquals(scale.getNote(0), detection.detect(440f))
        assertEquals(scale.getNote(0), detection.detect(f(0.45f)))
        assertEquals(scale.getNote(1), detection.detect(f(0.55f)))
        assertEquals(scale.getNote(-1), detection.detect(f(-0.55f)))
    }

    @Test
    fun chromaticKeepsPreviousNoteWithinHysteresisRange() {
        // 12-EDO: deviation based range is 60 cents, cent based range is min(20, 100 - 5) -> 60 cents
        val detection = TargetNoteAutoDetection(scale, null, 5f)
        val a4 = scale.getNote(0)
        assertEquals(a4, detection.detect(f(0.57f), a4))
        assertEquals(a4, detection.detect(f(-0.57f), a4))
    }

    @Test
    fun chromaticSwitchesNoteWhenLeavingHysteresisRange() {
        val detection = TargetNoteAutoDetection(scale, null, 5f)
        val a4 = scale.getNote(0)
        assertEquals(scale.getNote(1), detection.detect(f(0.63f), a4))
        assertEquals(scale.getNote(-1), detection.detect(f(-0.63f), a4))
        // far away from the previous note, the closest note is returned
        assertEquals(scale.getNote(7), detection.detect(f(7.1f), a4))
    }

    @Test
    fun denseScaleUsesCentBasedRangeClippedByTolerance() {
        val edo41 = MusicalScale2(
            predefinedTemperamentEDO(41, 1L),
            _rootNote = null,
            _referenceNote = null,
            referenceFrequency = 440f,
            frequencyMin = 30f,
            frequencyMax = 18000f,
            _stretchTuning = null
        )
        val centsPerStep = 1200f / 41f // ~29.27 cents
        val noteIndexOf19Cents = 19f / centsPerStep
        val noteIndexOf21Cents = 21f / centsPerStep
        val previous = edo41.getNote(0)
        val next = edo41.getNote(1)
        val frequency19Cents = edo41.getNoteFrequency(noteIndexOf19Cents)
        val frequency21Cents = edo41.getNoteFrequency(noteIndexOf21Cents)

        // tolerance 5 cents: range is max(0.6 * 29.27, min(20, 29.27 - 5)) = 20 cents
        val smallTolerance = TargetNoteAutoDetection(edo41, null, 5f)
        assertEquals(next, smallTolerance.detect(frequency19Cents))
        assertEquals(previous, smallTolerance.detect(frequency19Cents, previous))
        assertEquals(next, smallTolerance.detect(frequency21Cents, previous))

        // tolerance 15 cents: cent based range is clipped to 29.27 - 15 = 14.27 cents, so the
        // deviation based range of 0.6 * 29.27 = 17.56 cents dominates
        val largeTolerance = TargetNoteAutoDetection(edo41, null, 15f)
        val frequency17Cents = edo41.getNoteFrequency(17f / centsPerStep)
        assertEquals(previous, largeTolerance.detect(frequency17Cents, previous))
        assertEquals(next, largeTolerance.detect(frequency19Cents, previous))
    }

    @Test
    fun previousNoteNotPartOfScaleIsIgnored() {
        val chromatic = TargetNoteAutoDetection(scale, null, 5f)
        assertEquals(scale.getNote(-9), chromatic.detect(f(-8.9f), noteNotInScale))

        val strings = TargetNoteAutoDetection(scale, guitar, 5f)
        assertEquals(noteD3, strings.detect(f(-19.2f), noteNotInScale))
    }

    @Test
    fun invalidFrequencyReturnsNull() {
        assertNull(TargetNoteAutoDetection(scale, null, 5f).detect(0f))
        assertNull(TargetNoteAutoDetection(scale, null, 5f).detect(-100f, scale.getNote(0)))
        assertNull(TargetNoteAutoDetection(scale, guitar, 5f).detect(0f, noteA2))
    }

    @Test
    fun instrumentWithoutMatchingStringsReturnsNull() {
        assertNull(TargetNoteAutoDetection(scale, instrument(), 5f).detect(440f))
        val offScale = TargetNoteAutoDetection(scale, instrument(noteNotInScale), 5f)
        assertNull(offScale.detect(440f))
        assertNull(offScale.detect(440f, noteNotInScale))
    }

    @Test
    fun instrumentReturnsClosestString() {
        val detection = TargetNoteAutoDetection(scale, guitar, 5f)
        assertEquals(noteE2, detection.detect(f(-29f)))
        assertEquals(noteA2, detection.detect(f(-23f)))
        assertEquals(noteD3, detection.detect(f(-21.2f)))
        assertEquals(noteB3, detection.detect(f(-9f)))
        // previous note which is in the scale, but not a string, is ignored
        assertEquals(noteG3, detection.detect(f(-13f), scale.getNote(-12)))
    }

    @Test
    fun instrumentKeepsPreviousStringWithinHysteresisRange() {
        // A2 (-24) and D3 (-19): upper bound for A2 is at index 0.4 * -24 + 0.6 * -19 = -21
        val detection = TargetNoteAutoDetection(scale, guitar, 5f)
        assertEquals(noteA2, detection.detect(f(-21.2f), noteA2))
        assertEquals(noteD3, detection.detect(f(-20.8f), noteA2))
        // lower bound of D3 is at index 0.4 * -19 + 0.6 * -24 = -22
        assertEquals(noteD3, detection.detect(f(-21.8f), noteD3))
        assertEquals(noteA2, detection.detect(f(-22.2f), noteD3))
    }

    @Test
    fun lowestAndHighestStringHaveUnboundedOuterRange() {
        val detection = TargetNoteAutoDetection(scale, guitar, 5f)
        assertEquals(noteE2, detection.detect(f(-45f)))
        assertEquals(noteE2, detection.detect(f(-45f), noteE2))
        assertEquals(noteE2, detection.detect(f(-45f), noteE4))
        assertEquals(noteE4, detection.detect(f(30f)))
        assertEquals(noteE4, detection.detect(f(30f), noteE4))
        assertEquals(noteE4, detection.detect(f(30f), noteE2))
        // unbounded only to the outside
        assertEquals(noteA2, detection.detect(f(-24f), noteE2))
        assertEquals(noteB3, detection.detect(f(-10f), noteE4))
    }

    @Test
    fun stringsNotPartOfScaleNeverReturnSentinel() {
        // sorted note indices are [-24, -19, Int.MAX_VALUE]
        val instrument = instrument(noteD3, noteNotInScale, noteA2)
        val detection = TargetNoteAutoDetection(scale, instrument, 5f)
        assertEquals(noteD3, detection.detect(f(30f)))
        assertEquals(noteD3, detection.detect(f(30f), noteA2))
        assertEquals(noteD3, detection.detect(f(30f), noteNotInScale))
        assertEquals(noteD3, detection.detect(f(-20f), noteNotInScale))
        // highest valid string has unbounded upper range
        assertEquals(noteD3, detection.detect(f(30f), noteD3))
        assertEquals(noteA2, detection.detect(f(-30f), noteD3))
    }

    @Test
    fun singleDistinctStringIsAlwaysReturned() {
        val detection = TargetNoteAutoDetection(scale, instrument(noteA2, noteA2), 5f)
        assertEquals(noteA2, detection.detect(f(-40f)))
        assertEquals(noteA2, detection.detect(f(20f)))
        assertEquals(noteA2, detection.detect(f(20f), noteA2))
        assertEquals(noteA2, detection.detect(f(20f), scale.getNote(20)))
    }
}
