/*
* Copyright 2024 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.notenames

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/** Class containing the notes of one octave.
 * @param notes Array of the note names within the octave. The octave index of the note is of no
 *   importance here.
 * @param defaultReferenceNote The note which normally is used as reference note within tuning. Note
 *   that here, the octave index is important.
 */
@Serializable
@Immutable
data class NoteNames(val notes: Array<MusicalNote>, val defaultReferenceNote: MusicalNote) {
    /** Number of notes. */
    val size get() = notes.size

    /** Return note index within the notes-array.
     * @param note Note for which the index. Note that the octave index is ignored.
     * @return Index of note within the notes-array or -1 if note is not found.
     */
    fun getNoteIndex(note: MusicalNote): Int {
        val index = notes.indexOfFirst {
            it.equalsIgnoreOctave(note)
        }
        return index
    }

    /** Obtain note at given index.
     * @param index Index of note.
     * @return Note.
     */
    operator fun get(index: Int): MusicalNote = notes[index]

    /** Obtain note at given index if it exists else null.
     * @param index Index of note.
     * @return Note or null if it does not exist.
     */
    fun getOrNull(index: Int): MusicalNote? = notes.getOrNull(index)

    /** Get new note name scale, where base note and enharmonics are switched.
     * This will not change name, description or stableIds, since it effectively are the same
     * notes.
     * @return New note name scale where base notes are exchanged with the enharmonics.
     */
    fun switchEnharmonics(): NoteNames = NoteNames(
        notes.map { it.switchEnharmonic() }.toTypedArray(),
        defaultReferenceNote.switchEnharmonic()
    )

    /** Check if a given note is part of the note names array.
     * @param note Note. Octave index is ignored.
     * @return True if note is part of the note names array, else false.
     */
    fun hasNote(note: MusicalNote): Boolean = notes.any { it.equalsIgnoreOctave(note) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NoteNames
        if (!notes.contentEquals(other.notes)) return false
        if (defaultReferenceNote != other.defaultReferenceNote) return false

        return true
    }

    override fun hashCode(): Int {
        var result = notes.contentHashCode()
        result = 31 * result + defaultReferenceNote.hashCode()
        return result
    }
}

fun NoteNames.toNew(): NoteNames2 = NoteNames2(notes, defaultReferenceNote, notes[0])
