package de.moekadu.tuner

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.musicalscale.MusicalScale2
import de.moekadu.tuner.notedetection.SortedAndDistinctInstrumentStrings
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SortedAndDistinctInstrumentStringsTest {
    // reference note A4 = 440Hz has note index 0
    private val scale = MusicalScale2.createTestEdo12()

    /** Note which does not exist in a 12-EDO scale. */
    private val noteNotInScale = MusicalNote(BaseNote.C, NoteModifier.NaturalUp, octave = 4)

    private fun instrument(vararg strings: MusicalNote, isChromatic: Boolean = false) = Instrument(
        name = "test",
        nameResource = null,
        strings = arrayOf(*strings),
        icon = InstrumentIcon.entries[0],
        stableId = 1L,
        isChromatic = isChromatic
    )

    @Test
    fun stringsAreSortedAndMadeDistinct() {
        val instrument = instrument(
            scale.getNote(-5),
            scale.getNote(-24),
            scale.getNote(-5),
            scale.getNote(-29)
        )
        val strings = SortedAndDistinctInstrumentStrings(instrument, scale)
        assertEquals(listOf(-29, -24, -5), strings.sortedAndDistinctNoteIndices)
        assertEquals(3, strings.numDifferentNotes)
    }

    @Test
    fun stringsNotInScaleAreCollapsedIntoTrailingSentinel() {
        val strings = SortedAndDistinctInstrumentStrings(
            instrument(noteNotInScale, scale.getNote(-19), noteNotInScale, scale.getNote(-24)),
            scale
        )
        assertEquals(listOf(-24, -19, Int.MAX_VALUE), strings.sortedAndDistinctNoteIndices)
        assertEquals(2, strings.numDifferentNotes)

        val onlyInvalid = SortedAndDistinctInstrumentStrings(instrument(noteNotInScale), scale)
        assertEquals(listOf(Int.MAX_VALUE), onlyInvalid.sortedAndDistinctNoteIndices)
        assertEquals(0, onlyInvalid.numDifferentNotes)
    }

    @Test
    fun chromaticAndEmptyInstrumentsHaveNoStrings() {
        val chromatic = SortedAndDistinctInstrumentStrings(instrument(isChromatic = true), scale)
        assertEquals(0, chromatic.numDifferentNotes)
        val empty = SortedAndDistinctInstrumentStrings(instrument(), scale)
        assertEquals(0, empty.numDifferentNotes)
    }

    @Test
    fun notePartOfInstrument() {
        val strings = SortedAndDistinctInstrumentStrings(
            instrument(scale.getNote(-24), noteNotInScale),
            scale
        )
        assertTrue(strings.isNotePartOfInstrument(scale.getNote(-24)))
        assertFalse(strings.isNotePartOfInstrument(scale.getNote(-12)))
        assertFalse(strings.isNotePartOfInstrument(noteNotInScale))
        assertFalse(strings.isNotePartOfInstrument(null))

        val chromatic = SortedAndDistinctInstrumentStrings(instrument(isChromatic = true), scale)
        assertTrue(chromatic.isNotePartOfInstrument(scale.getNote(7)))
        assertFalse(chromatic.isNotePartOfInstrument(noteNotInScale))
        assertFalse(chromatic.isNotePartOfInstrument(null))

        val empty = SortedAndDistinctInstrumentStrings(instrument(), scale)
        assertFalse(empty.isNotePartOfInstrument(scale.getNote(0)))
    }
}
