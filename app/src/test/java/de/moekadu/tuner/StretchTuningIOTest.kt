package de.moekadu.tuner

import android.content.res.Resources
import de.moekadu.tuner.misc.FileCheck
import de.moekadu.tuner.misc.GetTextFromString
import de.moekadu.tuner.stretchtuning.StretchTuning
import de.moekadu.tuner.stretchtuning.StretchTuningIO
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class StretchTuningIOTest {
    @Test
    fun simple() {
        val string = """Studio ABC (1,4-2,0 m): C4 -16,1 -13,9 -11,8 -9,9 -6,9 -5,1 -8,4
                
Concert DEF (>4,7 m): A0 -6,5 -6 -5,5 -5 -4,6 -4,2 -3,8 -3,4 -3,1 -2,8 -2,5 -2,2 -1,9 -1,6


Stretch 5: F#4 -12,6 -12,25 -11,9 -11,55 -11,2 -10,85 -10,5 -10,15 -9,8 -9,45 -9,1 -8,75

Stretch M: Gb4 -2 -1,5 -1 -0,5 0 0 0 0 0 0 0 0,35 0,7 1,05 1,4 1,75 2,1

Stretch L: A0  -8,67 -8 -7,33 -6,67 -4 -3,33 0 0 0 0 0 0,5 1  8 8,5 9 9,5 13,5  17 17,5 18
"""
//        val tunings = StretchTuningIO.parseStretchTunings(string)
//        println("Number of tunings = ${tunings.size}")
//        for (t in tunings) {
//            println((t.name as GetTextFromString).string)
//            for (i in 0 until t.size)
//                println("${t.unstretchedFrequencies[i]} ${t.stretchInCents[i]}")
//        }
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
    fun stretchTuningsToStringRoundTrip() {
        val tuning = StretchTuning(
            name = GetTextFromString("Test tuning"),
            description = GetTextFromString("Test description"),
            unstretchedFrequencies = doubleArrayOf(110.0, 220.0, 440.0, 880.0),
            stretchInCents = doubleArrayOf(-5.0, -2.0, 0.0, 3.0)
        )

        val string = StretchTuningIO.stretchTuningsToString(unusedResources(), listOf(tuning))
        val result = StretchTuningIO.parseStretchTunings(string.reader().buffered())

        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(1, result.stretchTunings.size)
        val parsed = result.stretchTunings[0]
        assertEquals("Test tuning", parsed.name.value(null))
        assertEquals("Test description", parsed.description.value(null))
        assertArrayEquals(tuning.unstretchedFrequencies, parsed.unstretchedFrequencies, 1e-4)
        assertArrayEquals(tuning.stretchInCents, parsed.stretchInCents, 1e-4)
    }

    @Test
    fun readStretchTuningsReturnsInvalidIfStreamCannotBeOpened() {
        val result = StretchTuningIO.readStretchTunings {
            throw FileNotFoundException("missing file")
        }
        assertEquals(FileCheck.Invalid, result.fileCheck)
        assertEquals(0, result.stretchTunings.size)
    }

    @Test
    fun readStretchTuningsReturnsInvalidIfPermissionIsMissing() {
        val result = StretchTuningIO.readStretchTunings {
            throw SecurityException("permission revoked")
        }
        assertEquals(FileCheck.Invalid, result.fileCheck)
        assertEquals(0, result.stretchTunings.size)
    }

    @Test
    fun readStretchTuningsReturnsInvalidIfReadingFails() {
        val failingStream = object : InputStream() {
            override fun read(): Int = throw IOException("EIO")
        }
        val result = StretchTuningIO.readStretchTunings { failingStream }
        assertEquals(FileCheck.Invalid, result.fileCheck)
        assertEquals(0, result.stretchTunings.size)
    }
}
