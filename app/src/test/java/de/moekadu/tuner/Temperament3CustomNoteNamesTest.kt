package de.moekadu.tuner

import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.temperaments.EditableTemperament
import de.moekadu.tuner.temperaments.Temperament3
import de.moekadu.tuner.temperaments.Temperament3Custom
import de.moekadu.tuner.temperaments.toTemperament3Custom
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class Temperament3CustomNoteNamesTest {
    private val bases = arrayOf(
        BaseNote.C, BaseNote.D, BaseNote.E, BaseNote.F, BaseNote.G, BaseNote.A, BaseNote.B
    )

    /** Distinct, octave-less user note names for a scale with [notesPerOctave] notes. */
    private fun userNoteNames(notesPerOctave: Int) = Array(notesPerOctave) {
        MusicalNote(bases[it % bases.size], NoteModifier.None, octaveOffset = it / bases.size)
    }

    /** Editable temperament as produced by the file import: one line per note plus the octave line. */
    private fun editableTemperament(notes: Array<MusicalNote>): EditableTemperament {
        val notesPerOctave = notes.size
        return EditableTemperament(
            name = "test",
            abbreviation = "t",
            description = "",
            noteLines = Array(notesPerOctave + 1) {
                EditableTemperament.NoteLineContents(
                    note = notes[it % notesPerOctave].copy(octave = 4 + it / notesPerOctave),
                    cent = it * 1200.0 / notesPerOctave,
                    ratio = null
                )
            },
            stableId = Temperament3.NO_STABLE_ID
        )
    }

    private fun Array<MusicalNote>.withoutOctave() = map { it.copy(octave = Int.MAX_VALUE) }

    @Test
    fun importedTemperamentAboveGeneratorLimitUsesStoredNoteNames() {
        val notes = userNoteNames(100)
        val temperament = checkNotNull(editableTemperament(notes).toTemperament3Custom())

        val noteNames = temperament.noteNames(null)

        assertArrayEquals(notes.withoutOctave().toTypedArray(), noteNames.notes.withoutOctave().toTypedArray())
    }

    @Test
    fun importedTemperamentKeepsUserNoteNamesInsteadOfGeneratedOnes() {
        // 12 notes with names that differ from the predefined 12-EDO names
        val notes = userNoteNames(12)
        val temperament = checkNotNull(editableTemperament(notes).toTemperament3Custom())

        val noteNames = temperament.noteNames(null)

        assertArrayEquals(notes.withoutOctave().toTypedArray(), noteNames.notes.withoutOctave().toTypedArray())
    }

    @Test
    fun storedNoteNamesIncludingOctaveLineAreAccepted() {
        // Temperaments imported before the fix were persisted with size + 1 note names.
        val notesPerOctave = 100
        val notes = userNoteNames(notesPerOctave)
        val storedNotes = Array(notesPerOctave + 1) {
            notes[it % notesPerOctave].copy(octave = 4 + it / notesPerOctave)
        }
        val temperament = Temperament3Custom(
            _name = "test",
            _abbreviation = "t",
            _description = "",
            cents = DoubleArray(notesPerOctave + 1) { it * 1200.0 / notesPerOctave },
            _rationalNumbers = arrayOf(),
            _noteNames = storedNotes,
            stableId = Temperament3.NO_STABLE_ID
        )

        val noteNames = temperament.noteNames(null)

        assertArrayEquals(storedNotes.copyOf(notesPerOctave), noteNames.notes)
        assertArrayEquals(storedNotes.copyOf(notesPerOctave), temperament.possibleRootNotes())
    }
}
