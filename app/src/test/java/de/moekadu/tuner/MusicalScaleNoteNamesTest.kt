package de.moekadu.tuner

import de.moekadu.tuner.musicalscale.MusicalScaleNoteNames2
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.notenames.NoteNames2
import de.moekadu.tuner.notenames.NoteNamesEDOGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicalScaleNoteNamesTest {

    @Test
    fun testMusicalScaleIndexing() {
        val notes = NoteNames2(
            arrayOf(
                MusicalNote(BaseNote.F, NoteModifier.Sharp),
                MusicalNote(BaseNote.G, NoteModifier.None),
                MusicalNote(BaseNote.G, NoteModifier.Sharp),
                MusicalNote(BaseNote.A, NoteModifier.None),
                MusicalNote(BaseNote.A, NoteModifier.Sharp),
                MusicalNote(BaseNote.B, NoteModifier.None),
                MusicalNote(BaseNote.C, NoteModifier.None),
                MusicalNote(BaseNote.C, NoteModifier.Sharp),
                MusicalNote(BaseNote.D, NoteModifier.None),
                MusicalNote(BaseNote.D, NoteModifier.Sharp),
                MusicalNote(BaseNote.E, NoteModifier.None),
                MusicalNote(BaseNote.F, NoteModifier.None)
            ),
            MusicalNote(BaseNote.A, NoteModifier.None, 4),
            MusicalNote(BaseNote.C, NoteModifier.None)
        )

        val scale = MusicalScaleNoteNames2(notes, MusicalNote(BaseNote.A, NoteModifier.None, 4))

        assertEquals(-1, scale.getNoteIndex(MusicalNote(BaseNote.G, NoteModifier.Sharp, 4)))
        assertEquals(0, scale.getNoteIndex(MusicalNote(BaseNote.A, NoteModifier.None, 4)))
        assertEquals(1, scale.getNoteIndex(MusicalNote(BaseNote.A, NoteModifier.Sharp, 4)))
        assertEquals(2, scale.getNoteIndex(MusicalNote(BaseNote.B, NoteModifier.None, 4)))
        assertEquals(3, scale.getNoteIndex(MusicalNote(BaseNote.C, NoteModifier.None, 5)))
        assertEquals(4, scale.getNoteIndex(MusicalNote(BaseNote.C, NoteModifier.Sharp, 5)))
        assertEquals(16, scale.getNoteIndex(MusicalNote(BaseNote.C, NoteModifier.Sharp, 6)))

        for (someIndex in -100..100) {
            val someNote = scale.getNoteOfIndex(someIndex)
            val recoveredIndex = scale.getNoteIndex(someNote)
            println(someNote)
            assertEquals(someIndex, recoveredIndex)
        }
    }

    @Test
    fun testHasMatchingNoteAcceptsEnharmonicMatches() {
        val notes = NoteNamesEDOGenerator.getNoteNames(12, null)!!
        val scale = MusicalScaleNoteNames2(notes, MusicalNote(BaseNote.A, NoteModifier.None, 4))

        // scale note is C#/Db, query notes only define one of the two spellings
        val cSharp = MusicalNote(BaseNote.C, NoteModifier.Sharp, 4)
        val dFlat = MusicalNote(BaseNote.D, NoteModifier.Flat, 4)
        for (note in listOf(cSharp, dFlat)) {
            assertEquals(1, scale.getMatchingNoteIndices(note).size)
            assertTrue(scale.hasMatchingNote(note))
        }
        assertFalse(scale.hasMatchingNote(MusicalNote(BaseNote.C, NoteModifier.FlatFlat, 4)))
        assertFalse(scale.hasMatchingNote(null))
    }
}
