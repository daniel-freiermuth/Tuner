package de.moekadu.tuner.stretchtuning

import de.moekadu.tuner.misc.FileCheck
import de.moekadu.tuner.misc.GetTextFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class StretchTuningIOEmptyTuningTest {
    // Layout as produced by StretchTuningIO.writeStretchTunings: an empty tuning has only
    // its name and description lines, followed by the blank separator of the next tuning.
    @Test
    fun emptyTuningFollowedByOtherTuning() {
        val text = "# version=1\n" +
            "\n" +
            "# Empty\n" +
            "# d\n" +
            "\n" +
            "# Bass\n" +
            "# e\n" +
            "55.0000 -3.0000\n" +
            "110.0000 -1.0000\n"
        val result = parseStretchTuningsImpl(text.reader().buffered())

        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(2, result.stretchTunings.size)
        assertEquals("Empty", (result.stretchTunings[0].name as GetTextFromString).string)
        assertEquals(0, result.stretchTunings[0].size)
        assertEquals("Bass", (result.stretchTunings[1].name as GetTextFromString).string)
        assertEquals(2, result.stretchTunings[1].size)
    }

    @Test
    fun emptyTuningAtEndOfFile() {
        val text = "# version=1\n" +
            "\n" +
            "# Bass\n" +
            "# e\n" +
            "55.0000 -3.0000\n" +
            "\n" +
            "# Empty\n" +
            "# d\n"
        val result = parseStretchTuningsImpl(text.reader().buffered())

        assertEquals(FileCheck.Ok, result.fileCheck)
        assertEquals(2, result.stretchTunings.size)
        assertEquals("Bass", (result.stretchTunings[0].name as GetTextFromString).string)
        assertEquals("Empty", (result.stretchTunings[1].name as GetTextFromString).string)
        assertEquals(0, result.stretchTunings[1].size)
    }
}
