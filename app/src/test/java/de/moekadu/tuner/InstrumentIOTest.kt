package de.moekadu.tuner

import de.moekadu.tuner.instruments.Instrument
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.misc.FileCheck
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstrumentIOTest {
    private val e2 = MusicalNote(BaseNote.E, NoteModifier.None, 2)
    private val a2 = MusicalNote(BaseNote.A, NoteModifier.None, 2)
    private val fSharp4 = MusicalNote(BaseNote.F, NoteModifier.Sharp, 4)

    private fun stringsLine(vararg notes: MusicalNote) = "Strings=" +
        notes.joinToString(separator = ";", prefix = "[", postfix = "]") { it.asString() }

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

    @Test
    fun roundTripPreservesAllInstruments() {
        val instruments = listOf(
            Instrument("Guitar", null, arrayOf(e2, a2), InstrumentIcon.guitar, 3L),
            // name contains spaces, special characters, a newline and keyword-like text, which
            // must be read verbatim thanks to the "Length of name=" prefix
            Instrument(
                "My äöü #1 [x];\nIcon=piano",
                null,
                arrayOf(fSharp4),
                InstrumentIcon.double_bass,
                0L
            ),
            Instrument("Empty", null, arrayOf(), InstrumentIcon.piano, Instrument.NO_STABLE_ID)
        )

        val result = InstrumentIO.stringToInstruments(
            InstrumentIO.instrumentsListToString(null, instruments)
        )

        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(instruments, result.instruments)
    }

    @Test
    fun emptyStringIsEmpty() {
        val result = InstrumentIO.stringToInstruments("")
        assertEquals(FileCheck.Empty, result.fileCheck)
        assertEquals(0, result.instruments.size)
    }

    @Test
    fun textWithoutVersionAndInstrumentsIsInvalid() {
        // e.g. a temperament file or arbitrary text opened as instrument file
        val result = InstrumentIO.stringToInstruments("Some random\ntext Name=x\nStrings=\n")
        assertEquals(FileCheck.Invalid, result.fileCheck)
        assertEquals(0, result.instruments.size)
    }

    @Test
    fun versionWithoutInstrumentsIsOk() {
        val result = InstrumentIO.stringToInstruments("Version=7.0\n\n")
        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(0, result.instruments.size)
    }

    @Test
    fun legacyStringIndicesAreIgnoredAndInstrumentWithoutStringsIsDropped() {
        val input = "Version=6.0\n" +
            "Instrument 1\n" +
            "Name=Indices only\n" +
            "String indices=[0,1,2]\n" +
            "\n" +
            "Instrument 2\n" +
            "Name=Both\n" +
            "String indices=[0,1]\n" +
            stringsLine(e2, a2) + "\n"

        val result = InstrumentIO.stringToInstruments(input)

        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(1, result.instruments.size)
        val instrument = result.instruments[0]
        assertEquals("Both", instrument.getNameString(null))
        assertEquals(2L, instrument.stableId)
        assertArrayEquals(arrayOf(e2, a2), instrument.strings)
    }

    @Test
    fun missingOrNonNumericStableIdFallsBackToNoStableId() {
        val input = "Instrument abc\n" +
            "Name=First\n" +
            stringsLine(e2) + "\n" +
            "Instrument\n" +
            "Name=Second\n" +
            stringsLine(a2) + "\n"

        val result = InstrumentIO.stringToInstruments(input)

        // no version, but instruments were read
        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(
            listOf(Instrument.NO_STABLE_ID, Instrument.NO_STABLE_ID),
            result.instruments.map { it.stableId }
        )
    }

    @Test
    fun nameWithoutLengthIsReadUntilEndOfLineAndTrimmed() {
        val input = "Instrument 5\n" +
            "Name=  Bass guitar  \n" +
            "Icon=bass\n" +
            stringsLine(e2) + "\n"

        val result = InstrumentIO.stringToInstruments(input)

        assertEquals(1, result.instruments.size)
        assertEquals("Bass guitar", result.instruments[0].getNameString(null))
        assertEquals(InstrumentIcon.bass, result.instruments[0].icon)
    }

    @Test
    fun persistedIconNamesAreResolvedAndUnknownIconsFallBack() {
        val input = "Instrument 1\n" +
            "Name=A\n" +
            "Icon=double_bass\n" +
            stringsLine(e2) + "\n" +
            "Instrument 2\n" +
            "Name=B\n" +
            "Icon=kazoo\n" +
            stringsLine(e2) + "\n"

        val result = InstrumentIO.stringToInstruments(input)

        assertEquals(
            listOf(InstrumentIcon.double_bass, InstrumentIcon.entries[0]),
            result.instruments.map { it.icon }
        )
    }

    @Test
    fun nameLengthIsResetForNextInstrument() {
        val input = "Instrument 1\n" +
            "Length of name=3\n" +
            "Name=Uke\n" +
            stringsLine(a2) + "\n" +
            "Instrument 2\n" +
            "Name=Longer name\n" +
            stringsLine(e2) + "\n"

        val result = InstrumentIO.stringToInstruments(input)

        assertEquals(
            listOf("Uke", "Longer name"),
            result.instruments.map { it.getNameString(null) }
        )
    }
}
