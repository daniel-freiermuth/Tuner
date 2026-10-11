package de.moekadu.tuner

import de.moekadu.tuner.misc.FileCheck
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.temperaments.Temperament3Custom
import de.moekadu.tuner.temperaments.TemperamentIO
import de.moekadu.tuner.temperaments.toTemperament3Custom
import java.io.BufferedReader
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Test

class TemperamentImportTest {
    private fun import(scl: String): Temperament3Custom {
        val result = TemperamentIO.parseTemperaments(BufferedReader(StringReader(scl)))
        assertEquals(FileCheck.Ok, result.fileCheck)
        return result.temperaments.single().toTemperament3Custom()!!
    }

    private fun Temperament3Custom.importedNames() =
        noteNames(null).notes.map { Pair(it.base, it.modifier) }

    @Test
    fun importedCustomNoteNamesArePreserved() {
        val temperament = import(
            """
            ! pentatonic.scl
            !
            Pentatonic
            5
            !
             9/8    note=D
             5/4    note=E
             3/2    note=G
             5/3    note=A
             2/1    note=C
            """.trimIndent()
        )

        assertEquals(5, temperament.size)
        assertEquals(temperament.size, temperament.possibleRootNotes().size)
        val expected = listOf(BaseNote.C, BaseNote.D, BaseNote.E, BaseNote.G, BaseNote.A)
            .map { Pair(it, NoteModifier.None) }
        assertEquals(expected, temperament.importedNames())
    }

    @Test
    fun importedCustomTemperamentWithMoreThan72NotesKeepsItsNoteNames() {
        // no predefined EDO note names exist for more than 72 notes per octave
        val numberOfNotes = 73
        val modifiers = listOf(
            "" to NoteModifier.None,
            "#" to NoteModifier.Sharp,
            "b" to NoteModifier.Flat,
            "^" to NoteModifier.NaturalUp,
            "v" to NoteModifier.NaturalDown,
            "^#" to NoteModifier.SharpUp,
            "vb" to NoteModifier.FlatDown,
            "##" to NoteModifier.SharpSharp,
            "bb" to NoteModifier.FlatFlat,
            "^^" to NoteModifier.NaturalUpUp,
            "vv" to NoteModifier.NaturalDownDown
        )
        val baseNotes = BaseNote.entries.filter { it != BaseNote.None }
        val names = (0 until numberOfNotes).map {
            Pair(baseNotes[it % baseNotes.size], modifiers[it / baseNotes.size])
        }
        val lines = (1..numberOfNotes).joinToString("\n") {
            val (base, modifier) = names[it % numberOfNotes]
            " %.2f    note=%s%s".format(1200.0 * it / numberOfNotes, base.name, modifier.first)
        }
        val temperament = import("Many notes\n$numberOfNotes\n!\n$lines")

        assertEquals(numberOfNotes, temperament.size)
        assertEquals(temperament.size, temperament.possibleRootNotes().size)
        assertEquals(
            names.map { Pair(it.first, it.second.second) },
            temperament.importedNames()
        )
    }
}
