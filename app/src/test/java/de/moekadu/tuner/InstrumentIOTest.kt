package de.moekadu.tuner

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.misc.FileCheck
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentIOTest {
    private val exported = InstrumentIO.instrumentsListToString(
        null,
        listOf(
            Instrument(
                "a",
                null,
                arrayOf(MusicalNote(BaseNote.E, NoteModifier.None, 2)),
                InstrumentIcon.entries[0],
                1L
            ),
            Instrument(
                "b",
                null,
                arrayOf(
                    MusicalNote(BaseNote.A, NoteModifier.None, 2),
                    MusicalNote(BaseNote.D, NoteModifier.Sharp, 3)
                ),
                InstrumentIcon.entries[0],
                2L
            )
        )
    )

    private fun assertRejected(input: String) {
        val result = InstrumentIO.stringToInstruments(input)
        assertEquals(FileCheck.Invalid, result.fileCheck)
        assertTrue(result.instruments.isEmpty())
    }

    @Test
    fun unknownBaseNoteIsRejected() = assertRejected("Strings=[MusicalNote(base=X)]")

    @Test
    fun nonNoteArrayEntryIsRejected() = assertRejected("Strings=[foo]")

    @Test
    fun unterminatedArrayAtEndOfInputIsRejected() = assertRejected("Strings=[MusicalNote(base=C")

    @Test
    fun unterminatedArrayFollowedByMoreLinesIsRejected() =
        assertRejected("Strings=[MusicalNote(base=C\nName=x")

    @Test
    fun completeExportIsAccepted() {
        val result = InstrumentIO.stringToInstruments(exported)
        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(2, result.instruments.size)
    }

    @Test
    fun exportTruncatedInsideLastStringsArrayIsInvalid() {
        // export ends with "]\n"; drop both to cut the last array short
        val result = InstrumentIO.stringToInstruments(exported.dropLast(2))
        assertEquals(FileCheck.Invalid, result.fileCheck)
    }

    @Test
    fun exportWithMalformedNoteInsideStringsArrayIsInvalid() {
        val result = InstrumentIO.stringToInstruments(exported.replace("base=A", "base=X"))
        assertEquals(FileCheck.Invalid, result.fileCheck)
    }
}
