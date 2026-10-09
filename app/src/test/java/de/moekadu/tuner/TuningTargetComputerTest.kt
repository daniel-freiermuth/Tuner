package de.moekadu.tuner

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.notedetection.TuningTarget
import de.moekadu.tuner.notedetection.TuningTargetComputer
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import org.junit.Assert.assertEquals
import org.junit.Test

class TuningTargetComputerTest {
    // reference note A4 = 440Hz has note index 0
    private val scale = MusicalScale2.createTestEdo12()

    private val noteA2 = scale.getNote(-24)
    private val noteD3 = scale.getNote(-19)
    private val noteA4 = scale.getNote(0)
    private val noteASharp4 = scale.getNote(1)

    /** Note which does not exist in a 12-EDO scale. */
    private val noteNotInScale = MusicalNote(BaseNote.C, NoteModifier.NaturalUp, octave = 4)

    /** Note which matches two scale notes: A4 via its base and A#4 via its enharmonic. */
    private val noteA4OrASharp4 = MusicalNote(
        BaseNote.A,
        NoteModifier.None,
        octave = 4,
        enharmonicBase = BaseNote.A,
        enharmonicModifier = NoteModifier.Sharp
    )

    private fun instrument(vararg strings: MusicalNote) = Instrument(
        name = "test",
        nameResource = null,
        strings = arrayOf(*strings),
        icon = InstrumentIcon.entries[0],
        stableId = 1L
    )

    private val twoStrings = instrument(noteA2, noteD3)

    /** Frequency of a (possibly fractional) note index of the 12-EDO test scale. */
    private fun f(noteIndex: Float) = scale.getNoteFrequency(noteIndex)

    @Test
    fun invalidFrequencyReturnsReferenceNote() {
        assertEquals(
            TuningTarget(noteA4, 440f, isPartOfInstrument = true, instrumentHasNoStrings = false),
            TuningTargetComputer(scale, null, 5f)(0f, null, null)
        )
        assertEquals(
            TuningTarget(noteA4, 440f, isPartOfInstrument = false, instrumentHasNoStrings = false),
            TuningTargetComputer(scale, twoStrings, 5f)(-1f, noteD3, null)
        )
    }

    @Test
    fun detectedInstrumentStringIsReturned() {
        assertEquals(
            TuningTarget(
                noteD3,
                scale.getNoteFrequency(-19),
                isPartOfInstrument = true,
                instrumentHasNoStrings = false
            ),
            TuningTargetComputer(scale, twoStrings, 5f)(f(-18f), null, null)
        )
    }

    @Test
    fun instrumentWithoutUsableStringsFallsBackToChromatic() {
        assertEquals(
            TuningTarget(
                noteASharp4,
                scale.getNoteFrequency(1),
                isPartOfInstrument = false,
                instrumentHasNoStrings = true
            ),
            TuningTargetComputer(scale, instrument(), 5f)(f(0.9f), null, null)
        )
        assertEquals(
            TuningTarget(
                noteASharp4,
                scale.getNoteFrequency(1),
                isPartOfInstrument = false,
                instrumentHasNoStrings = false
            ),
            TuningTargetComputer(scale, instrument(noteNotInScale), 5f)(f(0.9f), null, null)
        )
    }

    @Test
    fun userDefinedTargetNoteOverridesDetection() {
        val computer = TuningTargetComputer(scale, twoStrings, 5f)
        assertEquals(
            TuningTarget(noteA4, 440f, isPartOfInstrument = false, instrumentHasNoStrings = false),
            computer(f(-19f), noteD3, noteA4)
        )
        assertEquals(
            TuningTarget(
                noteA2,
                scale.getNoteFrequency(-24),
                isPartOfInstrument = true,
                instrumentHasNoStrings = false
            ),
            computer(0f, null, noteA2)
        )
    }

    @Test
    fun userDefinedTargetNoteNotInScaleIsIgnored() {
        val target = TuningTargetComputer(scale, twoStrings, 5f)(f(-19f), null, noteNotInScale)
        assertEquals(noteD3, target.note)
    }

    @Test
    fun userDefinedTargetNoteWithTwoMatchesPicksClosestBiasedTowardsPreviousNote() {
        // A4 and A#4 are 100 cents apart, tolerance is min(100 / 4, 10) = 10 cents
        val computer = TuningTargetComputer(scale, null, 10f)
        val frequencyA4 = scale.getNoteFrequency(0)
        val frequencyASharp4 = scale.getNoteFrequency(1)

        assertEquals(frequencyA4, computer(f(0.47f), null, noteA4OrASharp4).frequency)
        assertEquals(frequencyASharp4, computer(f(0.53f), null, noteA4OrASharp4).frequency)
        assertEquals(
            frequencyASharp4,
            computer(f(0.47f), noteASharp4, noteA4OrASharp4).frequency
        )
        assertEquals(frequencyA4, computer(f(0.53f), noteA4, noteA4OrASharp4).frequency)
        // the returned note is the user defined note, not the matching scale note
        assertEquals(noteA4OrASharp4, computer(f(0.53f), null, noteA4OrASharp4).note)
    }

    @Test
    fun userDefinedTargetNoteBiasIsLimitedToQuarterOfNoteDistance() {
        // tolerance 50 cents is limited to 100 / 4 = 25 cents
        val computer = TuningTargetComputer(scale, null, 50f)
        val frequencyA4 = scale.getNoteFrequency(0)
        val frequencyASharp4 = scale.getNoteFrequency(1)
        assertEquals(frequencyA4, computer(f(0.6f), noteA4, noteA4OrASharp4).frequency)
        assertEquals(frequencyASharp4, computer(f(0.7f), noteA4, noteA4OrASharp4).frequency)
    }
}
