package de.moekadu.tuner.ui

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.notedetection.TargetNoteAutoDetection
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.temperaments.predefinedTemperamentPythagorean
import de.moekadu.tuner.ui.instruments.isStringHighlighted
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringsHighlightTest {
    private val edo12 = MusicalScale2.createTestEdo12()
    private val pythagorean = MusicalScale2(
        predefinedTemperamentPythagorean(1L),
        _rootNote = null,
        _referenceNote = null,
        referenceFrequency = 440f,
        frequencyMin = 30f,
        frequencyMax = 18000f,
        _stretchTuning = null
    )

    // strings created with 12-EDO, so they carry the F#/Gb enharmonic
    private val stringFSharp3 = edo12.getNote(-15)
    private val stringG3 = edo12.getNote(-14)

    private fun instrument(vararg strings: MusicalNote) = Instrument(
        name = "test",
        nameResource = null,
        strings = arrayOf(*strings),
        icon = InstrumentIcon.entries[0],
        stableId = 1L
    )

    /** Target note, which the tuner auto-detects for a string, when using the pythagorean scale. */
    private fun detectedTarget(string: MusicalNote, instrument: Instrument): MusicalNote {
        val noteIndex = pythagorean.getMatchingNoteIndices(string).single()
        val frequency = pythagorean.getNoteFrequency(noteIndex)
        return TargetNoteAutoDetection(pythagorean, instrument, 5f).detect(frequency)!!
    }

    @Test
    fun stringIsHighlightedForTargetNoteSpelledByScale() {
        val target = detectedTarget(stringFSharp3, instrument(stringFSharp3, stringG3))
        assertTrue(isStringHighlighted(0, stringFSharp3, null, target))
        assertFalse(isStringHighlighted(1, stringG3, null, target))
    }

    @Test
    fun highlightedKeyIsPreferredOverNote() {
        val target = detectedTarget(stringFSharp3, instrument(stringFSharp3, stringG3))
        assertFalse(isStringHighlighted(0, stringFSharp3, 1, target))
        assertTrue(isStringHighlighted(1, stringG3, 1, target))
    }
}
