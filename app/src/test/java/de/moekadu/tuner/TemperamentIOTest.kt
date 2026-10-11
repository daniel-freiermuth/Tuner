package de.moekadu.tuner

import de.moekadu.tuner.misc.FileCheck
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
}
