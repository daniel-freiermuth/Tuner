package de.moekadu.tuner

import android.content.res.Resources
import de.moekadu.tuner.misc.FileCheck
import de.moekadu.tuner.temperaments.RationalNumber
import de.moekadu.tuner.temperaments.Temperament3Custom
import de.moekadu.tuner.temperaments.TemperamentIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperamentIOTest {
    private fun assertRejected(input: String) {
        val result = TemperamentIO.parseTemperaments(input.reader().buffered())
        assertEquals(FileCheck.Invalid, result.fileCheck)
        assertTrue(result.temperaments.isEmpty())
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
}
