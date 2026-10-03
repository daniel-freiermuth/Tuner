package de.moekadu.tuner

import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.misc.FileCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentIOTest {
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
}
