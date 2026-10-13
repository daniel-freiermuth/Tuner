package de.moekadu.tuner

import android.content.res.Resources
import de.moekadu.tuner.misc.FileCheck
import de.moekadu.tuner.misc.GetTextFromString
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.temperaments.EditableTemperament
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments.Temperament3Custom
import de.moekadu.tuner.temperaments.Temperament3RationalNumbersEDONames
import de.moekadu.tuner.temperaments.TemperamentIO
import java.io.StringWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class TemperamentIOTest {
    private fun parse(content: String) =
        TemperamentIO.parseTemperaments(content.reader().buffered())

    private fun assertRejected(input: String) {
        val result = TemperamentIO.parseTemperaments(input.reader().buffered())
        assertEquals(FileCheck.Invalid, result.fileCheck)
        assertTrue(result.temperaments.isEmpty())
    }

    private fun write(vararg temperaments: Temperament3RationalNumbersEDONames): String {
        val stringWriter = StringWriter()
        val writer = stringWriter.buffered()
        TemperamentIO.writeTemperaments(temperaments.toList(), writer, unusedResources())
        writer.flush()
        return stringWriter.toString()
    }

    /** Compare the note name parts which survive a write/read cycle. The octave is not part of the
     * file format; the base note's octaveOffset is checked by baseNoteOctaveOffsetSurvivesRoundTrip.
     */
    private fun assertSameNoteName(expected: MusicalNote, actual: MusicalNote?) {
        val message = "expected $expected, got $actual"
        assertEquals(message, expected.base, actual?.base)
        assertEquals(message, expected.modifier, actual?.modifier)
        assertEquals(message, expected.enharmonicBase, actual?.enharmonicBase)
        assertEquals(message, expected.enharmonicModifier, actual?.enharmonicModifier)
        assertEquals(message, expected.enharmonicOctaveOffset, actual?.enharmonicOctaveOffset)
    }

    private fun singleNoteLine(line: String): EditableTemperament.NoteLineContents {
        val result = parse("Test\n1\n$line\n")
        assertEquals(FileCheck.Ok, result.fileCheck)
        // index 0 is the root copy of the octave line, index 1 is the parsed line itself
        return result.temperaments.single().noteLines[1]!!
    }

    private val justIntonation = Temperament3RationalNumbersEDONames(
        name = GetTextFromString("Just intonation"),
        abbreviation = GetTextFromString("JI"),
        description = GetTextFromString("Pure\nintervals"),
        rationalNumbers = arrayOf(
            RationalNumber(1, 1), RationalNumber(16, 15), RationalNumber(9, 8),
            RationalNumber(6, 5), RationalNumber(5, 4), RationalNumber(4, 3),
            RationalNumber(45, 32), RationalNumber(3, 2), RationalNumber(8, 5),
            RationalNumber(5, 3), RationalNumber(9, 5), RationalNumber(15, 8),
            RationalNumber(2, 1)
        ),
        stableId = 1L,
        uniqueIdentifier = "test-ji"
    )

    private val pythagoreanPentatonic = Temperament3RationalNumbersEDONames(
        name = GetTextFromString("Pentatonic"),
        abbreviation = GetTextFromString(""),
        description = GetTextFromString(""),
        rationalNumbers = arrayOf(
            RationalNumber(1, 1),
            RationalNumber(9, 8),
            RationalNumber(81, 64),
            RationalNumber(3, 2),
            RationalNumber(27, 16),
            RationalNumber(2, 1)
        ),
        stableId = 2L,
        uniqueIdentifier = "test-pentatonic"
    )

    @Test
    fun writtenTemperamentsAreReadBackWithSameValuesAndNoteNames() {
        val result = parse(write(justIntonation, pythagoreanPentatonic))

        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(2, result.temperaments.size)

        val originals = listOf(justIntonation, pythagoreanPentatonic)
        for ((original, parsed) in originals.zip(result.temperaments)) {
            val size = original.size
            val ratios = original.rationalNumbers()
            val noteNames = original.noteNames(null)
            assertEquals(size + 1, parsed.noteLines.size)

            // the root line is not written, it is restored as unison with the octave's note name
            assertEquals(RationalNumber(1, 1), parsed.noteLines[0]?.ratio)
            assertEquals(0.0, parsed.noteLines[0]?.cent)
            assertSameNoteName(noteNames[0], parsed.noteLines[0]?.note)

            for (i in 1..size) {
                val line = parsed.noteLines[i]
                assertEquals(ratios[i], line?.ratio)
                assertNull(line?.cent)
                assertSameNoteName(noteNames[i % size], line?.note)
            }
        }
    }

    @Test
    fun metadataIsReadBackAndResetBetweenTemperaments() {
        val (first, second) = parse(write(justIntonation, pythagoreanPentatonic)).temperaments

        assertEquals("Just intonation", first.name)
        assertEquals("JI", first.abbreviation)
        assertEquals("Pure intervals", first.description)

        // the second temperament has no abbreviation/description; values of the first must not leak
        assertEquals("Pentatonic", second.name)
        assertEquals("", second.abbreviation)
        assertEquals("", second.description)
    }

    @Test
    fun valuesAreReadAsCentsOnlyWithDecimalPoint() {
        val cent = singleNoteLine("701.955")
        assertEquals(701.955, cent.cent!!, 1e-12)
        assertNull(cent.ratio)

        val integer = singleNoteLine("2")
        assertEquals(RationalNumber(2, 1), integer.ratio)
        assertNull(integer.cent)

        val ratio = singleNoteLine("  3/2  ")
        assertEquals(RationalNumber(3, 2), ratio.ratio)
        assertNull(ratio.cent)
    }

    @Test
    fun noteModifiersMatchLongestNameFirst() {
        val expected = mapOf(
            "C" to NoteModifier.None,
            "C#" to NoteModifier.Sharp,
            "C##" to NoteModifier.SharpSharp,
            "Cvvv###" to NoteModifier.SharpSharpSharpDownDownDown,
            "C^#" to NoteModifier.SharpUp,
            "Cv" to NoteModifier.NaturalDown,
            "Cbb" to NoteModifier.FlatFlat,
            "C^^^bbb" to NoteModifier.FlatFlatFlatUpUpUp
        )
        for ((noteString, modifier) in expected) {
            val note = singleNoteLine("1.0 note=$noteString").note
            assertEquals(noteString, BaseNote.C, note?.base)
            assertEquals(noteString, modifier, note?.modifier)
            assertEquals(noteString, BaseNote.None, note?.enharmonicBase)
        }
    }

    @Test
    fun enharmonicIsReadAfterSlash() {
        val sharpFlat = singleNoteLine("100.0    note=C#/Db").note
        assertEquals(BaseNote.C, sharpFlat?.base)
        assertEquals(NoteModifier.Sharp, sharpFlat?.modifier)
        assertEquals(BaseNote.D, sharpFlat?.enharmonicBase)
        assertEquals(NoteModifier.Flat, sharpFlat?.enharmonicModifier)
        assertEquals(0, sharpFlat?.enharmonicOctaveOffset)

        // octave offsets of the base note must not disturb reading the enharmonic
        val withOffsets = singleNoteLine("1200.0 note=B#-1/C+1").note
        assertEquals(BaseNote.B, withOffsets?.base)
        assertEquals(NoteModifier.Sharp, withOffsets?.modifier)
        assertEquals(BaseNote.C, withOffsets?.enharmonicBase)
        assertEquals(NoteModifier.None, withOffsets?.enharmonicModifier)
        assertEquals(1, withOffsets?.enharmonicOctaveOffset)

        assertEquals(-2, singleNoteLine("1.0 note=C/Bb-2").note?.enharmonicOctaveOffset)
    }

    @Test
    fun unknownNoteKeyOrNameLeavesNoteEmptyButKeepsValue() {
        val wrongKey = singleNoteLine("3/2 name=G")
        assertEquals(RationalNumber(3, 2), wrongKey.ratio)
        assertNull(wrongKey.note)

        val unknownNote = singleNoteLine("3/2 note=H")
        assertEquals(RationalNumber(3, 2), unknownNote.ratio)
        assertNull(unknownNote.note)
    }

    @Test
    fun invalidNoteValueMakesWholeFileInvalid() {
        val valid = "Valid\n1\n2/1\n"
        for (value in listOf("abc", "3/2/1", "1,5", "x/2")) {
            val result = parse("${valid}Broken\n2\n3/2\n$value\n")
            assertEquals(value, FileCheck.Invalid, result.fileCheck)
            assertTrue(value, result.temperaments.isEmpty())
        }
    }

    @Test
    fun zeroOverZeroRatioIsRejected() = assertRejected("x\n1\n0/0\n")

    @Test
    fun minIntRatioWithoutSlashIsRejected() = assertRejected("x\n1\n-2147483648\n")

    @Test
    fun minIntNumeratorIsRejected() = assertRejected("x\n1\n-2147483648/1\n")

    @Test
    fun minIntDenominatorIsRejected() = assertRejected("x\n1\n1/-2147483648\n")

    @Test
    fun validRatioIsAccepted() {
        val result = TemperamentIO.parseTemperaments("x\n1\n2/1\n".reader().buffered())
        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(1, result.temperaments.size)
    }

    /** Resources instance without running the (stubbed) android constructor. Only used for
     * GetTextFromString, which never touches the resources.
     */
    private fun unusedResources(): Resources {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val unsafeField = unsafeClass.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null)
        val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
        return allocateInstance.invoke(unsafe, Resources::class.java) as Resources
    }

    @Test
    fun temperamentsListToStringRoundTrip() {
        val cents = DoubleArray(13) { 100.0 * it }
        val temperament = Temperament3Custom(
            _name = "Test temperament",
            _abbreviation = "TT",
            _description = "Test description",
            cents = cents,
            _rationalNumbers = arrayOfNulls<RationalNumber>(0),
            _noteNames = null,
            stableId = 1L
        )

        val string = TemperamentIO.temperamentsListToString(unusedResources(), listOf(temperament))
        val result = TemperamentIO.parseTemperaments(string.reader().buffered())

        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(1, result.temperaments.size)
        val parsed = result.temperaments[0]
        assertEquals("Test temperament", parsed.name)
        assertEquals("TT", parsed.abbreviation)
        assertEquals("Test description", parsed.description)
        assertEquals(1200.0, parsed.noteLines.last()?.cent ?: Double.NaN, 1e-2)
    }

    @Test
    fun invalidNumberOfNotesMakesFileInvalid() {
        for (count in listOf("-1", "1.5", "twelve")) {
            val result = parse("Name\n$count\n2/1\n")
            assertEquals(count, FileCheck.Invalid, result.fileCheck)
            assertTrue(count, result.temperaments.isEmpty())
        }
    }

    @Test
    fun temperamentWithZeroNotesIsSkipped() {
        assertEquals(FileCheck.Empty, parse("Nothing\n0\n").fileCheck)

        val result = parse("Nothing\n0\nOctave\n1\n2/1\n")
        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals("Octave", result.temperaments.single().name)
    }

    @Test
    fun filesWithoutCompleteTemperamentAreEmpty() {
        assertEquals(FileCheck.Empty, parse("").fileCheck)
        assertEquals(FileCheck.Empty, parse("! version=1.0\n!\n  ! description=x\n").fileCheck)

        // fewer note lines than announced: the incomplete temperament is not imported
        val truncated = parse("Truncated\n3\n9/8\n5/4\n")
        assertEquals(FileCheck.Empty, truncated.fileCheck)
        assertTrue(truncated.temperaments.isEmpty())
    }

    @Test
    fun byteOrderMarkAndCommentsAreIgnored() {
        val result =
            parse("\uFEFFWith BOM\n! comment between name and count\n2\n!\n3/2\n! inside\n2/1\n")
        assertEquals(FileCheck.Ok, result.fileCheck)
        val temperament = result.temperaments.single()
        assertEquals("With BOM", temperament.name)
        assertEquals(
            listOf(RationalNumber(1, 1), RationalNumber(3, 2), RationalNumber(2, 1)),
            temperament.noteLines.map { it?.ratio }
        )
    }

    @Ignore(
        "parseNote passes the offset positionally into MusicalNote.octave instead of octaveOffset"
    )
    @Test
    fun baseNoteOctaveOffsetSurvivesRoundTrip() {
        val noteNames = pythagoreanPentatonic.noteNames(null)
        // 5-EDO names contain B-1/C, so offsets are exercised by a plain export
        assertTrue(noteNames.notes.any { it.octaveOffset != 0 })
        val parsed = parse(write(pythagoreanPentatonic)).temperaments.single()
        for (i in 1..pythagoreanPentatonic.size) {
            val expected = noteNames[i % pythagoreanPentatonic.size]
            assertEquals(
                expected.toString(),
                expected.octaveOffset,
                parsed.noteLines[i]?.note?.octaveOffset
            )
        }
    }

    @Ignore("writer emits '-/Eb' for notes without base, parseNote only accepts '-<whitespace>/'")
    @Test
    fun noteWithoutBaseButWithEnharmonicIsRead() {
        // format produced by TemperamentIO.noteToString for MusicalNote(base = BaseNote.None, ...)
        val note = singleNoteLine("1.0 note=-/Eb").note
        assertEquals(BaseNote.None, note?.base)
        assertEquals(BaseNote.E, note?.enharmonicBase)
        assertEquals(NoteModifier.Flat, note?.enharmonicModifier)
    }
}
