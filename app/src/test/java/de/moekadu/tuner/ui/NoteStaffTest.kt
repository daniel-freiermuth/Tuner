package de.moekadu.tuner.ui

import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.ui.notes.GrandStaffPlacement
import de.moekadu.tuner.ui.notes.SpelledNote
import de.moekadu.tuner.ui.notes.grandStaffPlacement
import de.moekadu.tuner.ui.notes.ledgerLinePositions
import de.moekadu.tuner.ui.notes.spellNote
import de.moekadu.tuner.ui.notes.staffPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteStaffTest {
    private val cSharp4 = MusicalNote(
        BaseNote.C,
        NoteModifier.Sharp,
        4,
        enharmonicBase = BaseNote.D,
        enharmonicModifier = NoteModifier.Flat
    )
    private val dFlat4 = cSharp4.switchEnharmonic()
    private val aSharp4 = MusicalNote(
        BaseNote.A,
        NoteModifier.Sharp,
        4,
        enharmonicBase = BaseNote.B,
        enharmonicModifier = NoteModifier.Flat
    )
    private val bFlat4 = aSharp4.switchEnharmonic()
    private val f4 = MusicalNote(BaseNote.F, NoteModifier.None, 4)
    private val c4 = MusicalNote(
        BaseNote.C,
        NoteModifier.None,
        4,
        enharmonicBase = BaseNote.B,
        enharmonicModifier = NoteModifier.Sharp,
        enharmonicOctaveOffset = -1
    )

    @Test
    fun staffPositionUsesPrintedOctave() {
        // middle C
        assertEquals(28, MusicalNote(BaseNote.C, NoteModifier.None, 4).staffPosition())
        // Cb4 belongs to octave 3 of the scale but is printed in octave 4
        val cFlat4 = MusicalNote(BaseNote.C, NoteModifier.Flat, 3, octaveOffset = 1)
        assertEquals(28, cFlat4.staffPosition())
        // B#3 belongs to octave 4 but is printed in octave 3
        assertEquals(27, c4.switchEnharmonic().staffPosition())
        assertEquals(29, dFlat4.staffPosition())
    }

    @Test
    fun staffPositionNeedsBaseAndOctave() {
        assertNull(MusicalNote(BaseNote.None, NoteModifier.None, 4).staffPosition())
        assertNull(MusicalNote(BaseNote.C, NoteModifier.None).staffPosition())
    }

    @Test
    fun spellingWithoutKeySignatureFollowsEnharmonicPreference() {
        assertEquals(SpelledNote(cSharp4, true), spellNote(cSharp4, false, null))
        assertEquals(SpelledNote(dFlat4, true), spellNote(cSharp4, true, null))
        assertEquals(SpelledNote(f4, false), spellNote(f4, true, null))
    }

    @Test
    fun spellingPrefersNotesOfTheKey() {
        // D major: C# is part of the key, no accidental; F needs a natural sign
        assertEquals(SpelledNote(cSharp4, false), spellNote(cSharp4, true, 2))
        assertEquals(SpelledNote(f4, true), spellNote(f4, false, 2))
        // F major: Bb, regardless of the enharmonic preference
        assertEquals(SpelledNote(bFlat4, false), spellNote(aSharp4, false, -1))
        // Bb major: C#/Db is not in the key -> flat spelling with accidental
        assertEquals(SpelledNote(dFlat4, true), spellNote(cSharp4, false, -2))
        // G major: C#/Db not in the key -> sharp spelling
        assertEquals(SpelledNote(cSharp4, true), spellNote(dFlat4, false, 1))
        // C major: preference decides
        assertEquals(SpelledNote(dFlat4, true), spellNote(cSharp4, true, 0))
        assertEquals(SpelledNote(c4, false), spellNote(c4, true, 0))
        // C# major: C is written as B#
        assertEquals(SpelledNote(c4.switchEnharmonic(), false), spellNote(c4, false, 7))
    }

    @Test
    fun placementShiftsOnlyOutsideLedgerRange() {
        // E6 and A1 are the outermost notes drawn with ledger lines
        assertEquals(GrandStaffPlacement(44, 0), grandStaffPlacement(44))
        assertEquals(GrandStaffPlacement(12, 0), grandStaffPlacement(12))
        // F6 -> 8va
        assertEquals(GrandStaffPlacement(38, 1), grandStaffPlacement(45))
        // C8 -> 15ma
        assertEquals(GrandStaffPlacement(42, 2), grandStaffPlacement(56))
        // A0 -> 8vb
        assertEquals(GrandStaffPlacement(12, -1), grandStaffPlacement(5))
        // G1 -> 8vb
        assertEquals(GrandStaffPlacement(17, -1), grandStaffPlacement(10))
    }

    @Test
    fun ledgerLines() {
        assertEquals(listOf(28), ledgerLinePositions(28).toList()) // middle C
        assertEquals(emptyList<Int>(), ledgerLinePositions(27).toList()) // B3 above bass staff
        assertEquals(emptyList<Int>(), ledgerLinePositions(29).toList()) // D4 below treble staff
        assertEquals(emptyList<Int>(), ledgerLinePositions(39).toList()) // G5 on top of staff
        assertEquals(listOf(40), ledgerLinePositions(41).toList()) // B5
        assertEquals(listOf(40, 42, 44), ledgerLinePositions(44).toList())
        assertEquals(emptyList<Int>(), ledgerLinePositions(17).toList()) // F2 below staff
        assertEquals(listOf(16, 14), ledgerLinePositions(13).toList())
        assertEquals(listOf(16, 14, 12), ledgerLinePositions(12).toList())
    }
}
