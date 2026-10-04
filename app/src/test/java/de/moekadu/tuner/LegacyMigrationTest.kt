package de.moekadu.tuner

import de.moekadu.tuner.instruments.InstrumentIcon
import de.moekadu.tuner.instruments.InstrumentOld
import de.moekadu.tuner.instruments.instrumentDatabase
import de.moekadu.tuner.misc.StringOrResId
import de.moekadu.tuner.musicalscale.MusicalScale
import de.moekadu.tuner.notenames.BaseNote
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.notenames.NoteModifier
import de.moekadu.tuner.notenames.NoteNames
import de.moekadu.tuner.stretchtuning.StretchTuning
import de.moekadu.tuner.temperaments.Temperament
import de.moekadu.tuner.temperaments.Temperament3Custom
import de.moekadu.tuner.temperaments.Temperament3EDO
import de.moekadu.tuner.temperaments.predefinedTemperaments
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyMigrationTest {
    private val predefined = predefinedTemperaments()
    private val minPredefinedKey = predefined.minOf { it.stableId }

    /** Stable id of the n-EDO entry in TemperamentResources.edoTemperaments (minEdo = 5). */
    private fun edoSectionStableId(notesPerOctave: Int) =
        minPredefinedKey - 1 - (notesPerOctave - 5)

    private fun enharmonic(base: BaseNote, modifier: NoteModifier, other: BaseNote) = MusicalNote(
        base = base,
        modifier = modifier,
        enharmonicBase = other,
        enharmonicModifier =
            if (modifier == NoteModifier.Sharp) NoteModifier.Flat else NoteModifier.Sharp
    )

    private val noteNames12 = NoteNames(
        arrayOf(
            MusicalNote(BaseNote.C, NoteModifier.None),
            enharmonic(BaseNote.C, NoteModifier.Sharp, BaseNote.D),
            MusicalNote(BaseNote.D, NoteModifier.None),
            enharmonic(BaseNote.E, NoteModifier.Flat, BaseNote.D),
            MusicalNote(BaseNote.E, NoteModifier.None),
            MusicalNote(BaseNote.F, NoteModifier.None),
            enharmonic(BaseNote.F, NoteModifier.Sharp, BaseNote.G),
            MusicalNote(BaseNote.G, NoteModifier.None),
            enharmonic(BaseNote.A, NoteModifier.Flat, BaseNote.G),
            MusicalNote(BaseNote.A, NoteModifier.None),
            enharmonic(BaseNote.B, NoteModifier.Flat, BaseNote.A),
            MusicalNote(BaseNote.B, NoteModifier.None)
        ),
        MusicalNote(BaseNote.A, NoteModifier.None, 4)
    )

    private fun legacyPredefined(cents: DoubleArray, equalOctaveDivision: Int?) = Temperament(
        name = StringOrResId(R.string.equal_temperament_x),
        abbreviation = StringOrResId(R.string.equal_temperament_x_abbr),
        description = StringOrResId(R.string.equal_temperament_x),
        cents = cents,
        rationalNumbers = null,
        circleOfFifths = null,
        equalOctaveDivision = equalOctaveDivision,
        stableId = -1L
    )

    private fun edoCents(notesPerOctave: Int) =
        DoubleArray(notesPerOctave + 1) { it * 1200.0 / notesPerOctave }

    private fun legacyScale(
        temperament: Temperament,
        rootNote: MusicalNote,
        referenceNote: MusicalNote
    ) = MusicalScale(
        temperament = temperament,
        noteNames = noteNames12,
        rootNote = rootNote,
        referenceNote = referenceNote,
        referenceFrequency = 442f,
        frequencyMin = 16f,
        frequencyMax = 16000f,
        stretchTuning = StretchTuning()
    )

    @Test
    fun predefinedEdoMapsToEdoSectionStableId() {
        val predefinedIds = predefined.map { it.stableId }.toSet()
        val migratedIds = (5..72).map { edo ->
            val migrated = legacyPredefined(edoCents(edo), edo).toNew(noteNames12)
            assertTrue(migrated is Temperament3EDO)
            assertEquals(edo, (migrated as Temperament3EDO).notesPerOctave)
            assertEquals("stable id of $edo-EDO", edoSectionStableId(edo), migrated.stableId)
            assertTrue("$edo-EDO id must not be a custom (non-negative) id", migrated.stableId < 0)
            assertTrue(
                "$edo-EDO id ${migrated.stableId} collides with a predefined temperament",
                migrated.stableId !in predefinedIds
            )
            migrated.stableId
        }
        assertEquals(migratedIds.size, migratedIds.toSet().size)
    }

    @Test
    fun predefinedNonEdoMatchedByCents() {
        val pythagorean = predefined[1]
        val migrated = legacyPredefined(pythagorean.cents(), null).toNew(noteNames12)
        assertEquals(pythagorean.stableId, migrated.stableId)
        assertSame(pythagorean.javaClass, migrated.javaClass)
        assertArrayEquals(pythagorean.cents(), migrated.cents(), 1e-9)
    }

    @Test
    fun predefinedNonEdoWithoutMatchFallsBackTo12Edo() {
        val cents = edoCents(12).also { it[3] += 5.0 }
        val migrated = legacyPredefined(cents, null).toNew(noteNames12)
        assertEquals(Temperament3EDO(edoSectionStableId(12), 12), migrated)
    }

    @Test
    fun customEdoRegeneratesCentsAndKeepsIdentity() {
        val legacy = Temperament(
            name = StringOrResId("My EDO"),
            abbreviation = StringOrResId("ME"),
            description = StringOrResId("desc"),
            cents = doubleArrayOf(0.0, 100.0), // stale, must be ignored for EDO
            rationalNumbers = null,
            circleOfFifths = null,
            equalOctaveDivision = 12,
            stableId = 4711L
        )
        val migrated = legacy.toNew(noteNames12)
        assertTrue(migrated is Temperament3Custom)
        migrated as Temperament3Custom
        assertEquals(4711L, migrated.stableId)
        assertEquals("My EDO", migrated._name)
        assertEquals("ME", migrated._abbreviation)
        assertEquals("desc", migrated._description)
        assertArrayEquals(edoCents(12), migrated.cents(), 1e-9)
        assertEquals(noteNames12.size, migrated.size)
        assertArrayEquals(noteNames12.notes, migrated.possibleRootNotes())
    }

    @Test
    fun customNonEdoKeepsCents() {
        val cents = doubleArrayOf(0.0, 90.0, 204.0, 1200.0)
        val names = NoteNames(
            noteNames12.notes.copyOfRange(0, 3),
            MusicalNote(BaseNote.C, NoteModifier.None, 4)
        )
        val legacy = Temperament(
            name = StringOrResId("Mine"),
            abbreviation = StringOrResId("M"),
            description = StringOrResId(""),
            cents = cents,
            rationalNumbers = null,
            circleOfFifths = null,
            equalOctaveDivision = null,
            stableId = 3L
        )
        val migrated = legacy.toNew(names) as Temperament3Custom
        assertEquals(3L, migrated.stableId)
        assertArrayEquals(cents, migrated.cents(), 0.0)
    }

    @Test
    fun musicalScaleKeepsResolvableRootAndReferenceNote() {
        val root = MusicalNote(BaseNote.D, NoteModifier.None)
        val reference = MusicalNote(BaseNote.G, NoteModifier.None, 4)
        val migrated = legacyScale(legacyPredefined(edoCents(12), 12), root, reference).toNew()

        assertEquals(Temperament3EDO(edoSectionStableId(12), 12), migrated.temperament)
        assertTrue(migrated._rootNote!!.match(root, ignoreOctave = true))
        assertEquals(reference, migrated._referenceNote)
        assertEquals(442f, migrated.referenceFrequency)
        assertEquals(16f, migrated.frequencyMin)
        assertEquals(16000f, migrated.frequencyMax)
    }

    @Test
    fun musicalScaleUnresolvableRootAndReferenceFallBack() {
        val root = MusicalNote(BaseNote.C, NoteModifier.NaturalUp)
        val reference = MusicalNote(BaseNote.A, NoteModifier.NaturalUp, 4)
        val migrated = legacyScale(legacyPredefined(edoCents(12), 12), root, reference).toNew()

        assertNull(migrated._rootNote)
        val expectedReference = migrated.temperament.noteNames(null).defaultReferenceNote
        assertEquals(expectedReference, migrated._referenceNote)
    }

    @Test
    fun predefinedInstrumentMatchedFromDatabase() {
        val guitar = instrumentDatabase.first {
            it.icon == InstrumentIcon.guitar && !it.isChromatic
        }
        val legacy = InstrumentOld(
            name = null,
            nameResource = 12345,
            strings = guitar.strings.copyOf(),
            icon = guitar.icon,
            stableId = 99L,
            isChromatic = false
        )
        assertSame(guitar, legacy.toNew())
    }

    @Test
    fun predefinedInstrumentWithoutMatchFallsBackToFirstDatabaseEntry() {
        val legacy = InstrumentOld(
            name = null,
            nameResource = 12345,
            strings = arrayOf(MusicalNote(BaseNote.C, NoteModifier.None, 1)),
            icon = InstrumentIcon.harp,
            stableId = 99L,
            isChromatic = false
        )
        assertSame(instrumentDatabase[0], legacy.toNew())
    }

    @Test
    fun customInstrumentKeepsNameAndStableId() {
        val strings = arrayOf(
            MusicalNote(BaseNote.D, NoteModifier.None, 3),
            MusicalNote(BaseNote.A, NoteModifier.None, 3)
        )
        val migrated = InstrumentOld(
            name = "My instrument",
            nameResource = null,
            strings = strings,
            icon = InstrumentIcon.violin,
            stableId = 42L,
            isChromatic = false
        ).toNew()
        assertEquals("My instrument", migrated.getNameString(null))
        assertEquals(42L, migrated.stableId)
        assertTrue(!migrated.isPredefined())
        assertArrayEquals(strings, migrated.strings)
        assertEquals(InstrumentIcon.violin, migrated.icon)
    }

    @Test
    fun legacyJsonDecodesAndMigrates() {
        val scaleJson = """
            {
              "temperament": {
                "name": {"string": null, "resId": ${R.string.equal_temperament_x}},
                "abbreviation": {"string": null, "resId": ${R.string.equal_temperament_x_abbr}},
                "description": {"string": null, "resId": ${R.string.equal_temperament_x}},
                "cents": [${edoCents(19).joinToString(",")}],
                "rationalNumbers": null,
                "circleOfFifths": null,
                "equalOctaveDivision": 19,
                "stableId": -3
              },
              "noteNames": {
                "notes": [{"base": "C", "modifier": "None"}, {"base": "A", "modifier": "None"}],
                "defaultReferenceNote": {"base": "A", "modifier": "None", "octave": 4}
              },
              "rootNote": {"base": "C", "modifier": "None"},
              "referenceNote": {"base": "A", "modifier": "None", "octave": 4},
              "referenceFrequency": 440.0,
              "frequencyMin": 16.0,
              "frequencyMax": 16000.0,
              "stretchTuning": {}
            }
        """.trimIndent()
        val scale = Json.decodeFromString<MusicalScale>(scaleJson).toNew()
        assertEquals(Temperament3EDO(edoSectionStableId(19), 19), scale.temperament)
        assertEquals(440f, scale.referenceFrequency)

        val instrumentJson = """
            {
              "name": "Custom",
              "nameResource": null,
              "strings": [{"base": "E", "modifier": "None", "octave": 2}],
              "icon": "bass",
              "stableId": 7,
              "isChromatic": false
            }
        """.trimIndent()
        val instrument = Json.decodeFromString<InstrumentOld>(instrumentJson).toNew()
        assertEquals("Custom", instrument.getNameString(null))
        assertEquals(7L, instrument.stableId)
        assertEquals(InstrumentIcon.bass, instrument.icon)
    }
}
