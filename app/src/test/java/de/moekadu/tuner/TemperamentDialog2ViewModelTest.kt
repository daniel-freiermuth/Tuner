package de.moekadu.tuner

import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.temperaments.Temperament3EDO
import de.moekadu.tuner.viewmodels.proposeRootNoteFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperamentDialog2ViewModelTest {
    private val edo12 = Temperament3EDO(-1L, 12)
    private val edo19 = Temperament3EDO(-2L, 19)

    @Test
    fun keepsCurrentRootNoteIfPossibleInNewTemperament() {
        // every non-first root note of 12-EDO must be kept when staying within 12-EDO
        for (note in edo12.possibleRootNotes().drop(1)) {
            assertEquals(note, proposeRootNoteFor(edo12, note))
        }
    }

    @Test
    fun fallsBackToFirstPossibleRootNoteIfNotAvailable() {
        // 12-EDO C#/Db carries an enharmonic, which no 19-EDO root note does
        val rootNote = edo12.possibleRootNotes().first { it.enharmonicBase != BaseNote.None }
        assertFalse(edo19.possibleRootNotes().any { it.equalsIgnoreOctave(rootNote) })
        assertEquals(edo19.possibleRootNotes()[0], proposeRootNoteFor(edo19, rootNote))
    }

    @Test
    fun proposedNoteIsAlwaysPossibleRootNote() {
        for (target in listOf(edo12, edo19)) {
            for (current in edo12.possibleRootNotes() + edo19.possibleRootNotes()) {
                val proposed = proposeRootNoteFor(target, current)
                assertTrue(target.possibleRootNotes().any { it.equalsIgnoreOctave(proposed) })
            }
        }
    }

    @Test
    fun ignoresOctaveOfCurrentRootNote() {
        val note = edo12.possibleRootNotes()[2].copy(octave = 3)
        assertEquals(note, proposeRootNoteFor(edo12, note))
    }
}
