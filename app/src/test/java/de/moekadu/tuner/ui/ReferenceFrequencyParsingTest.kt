package de.moekadu.tuner.ui

import de.moekadu.tuner.ui.preferences.toFloatOrNull
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReferenceFrequencyParsingTest {
    private val format = NumberFormat.getNumberInstance(Locale.US) as DecimalFormat

    @Test
    fun parsesNumbersSurroundedByWhiteSpace() {
        assertEquals(440f, format.toFloatOrNull(" 440 "))
        assertEquals(442.5f, format.toFloatOrNull("442.5"))
    }

    @Test
    fun rejectsUnparseableInput() {
        assertNull(format.toFloatOrNull(""))
        assertNull(format.toFloatOrNull("440Hz"))
    }

    @Test
    fun rejectsValuesWithoutFiniteFloatRepresentation() {
        // An infinite reference frequency yields a musical scale without frequencies, which
        // crashes every tuner screen once persisted. A 40-digit number overflows Float.
        assertNull(format.toFloatOrNull("1" + "0".repeat(40)))
        assertNull(format.toFloatOrNull("\u221E"))
        assertNull(format.toFloatOrNull("-\u221E"))
        assertNull(format.toFloatOrNull("NaN"))
    }
}
