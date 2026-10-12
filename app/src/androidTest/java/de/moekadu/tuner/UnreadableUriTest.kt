package de.moekadu.tuner

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.moekadu.tuner.instruments.InstrumentIO
import de.moekadu.tuner.misc.FileCheck
import de.moekadu.tuner.misc.getFilenameFromUri
import de.moekadu.tuner.stretchtuning.StretchTuningIO
import de.moekadu.tuner.temperaments.TemperamentIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A file uri, which cannot be read (e.g. a VIEW intent re-delivered after its uri grant is gone)
 * must be reported as FileCheck.Invalid and the error toast must be able to obtain its (missing)
 * filename without crashing.
 */
@RunWith(AndroidJUnit4::class)
class UnreadableUriTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** Document uri, for which the app holds no grant, querying or opening it throws a SecurityException. */
    private val ungrantedUri =
        Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2Fx.txt")

    /** MediaStore uri without a row: opening throws FileNotFoundException, query gives an empty cursor. */
    private val missingRowUri = Uri.parse("content://media/external/file/2147483646")

    @Test
    fun ungrantedUriIsReportedAsInvalid() {
        assertEquals(FileCheck.Invalid, InstrumentIO.readInstrumentsFromFile(context, ungrantedUri).fileCheck)
        assertEquals(FileCheck.Invalid, TemperamentIO.readTemperamentsFromFile(context, ungrantedUri).fileCheck)
        assertEquals(FileCheck.Invalid, StretchTuningIO.readStretchTuningsFromFile(context, ungrantedUri).fileCheck)
    }

    @Test
    fun filenameOfUngrantedUriIsUnknown() {
        assertNull(getFilenameFromUri(context, ungrantedUri))
    }

    @Test
    fun missingFileIsReportedAsInvalid() {
        assertEquals(FileCheck.Invalid, InstrumentIO.readInstrumentsFromFile(context, missingRowUri).fileCheck)
        assertEquals(FileCheck.Invalid, TemperamentIO.readTemperamentsFromFile(context, missingRowUri).fileCheck)
        assertEquals(FileCheck.Invalid, StretchTuningIO.readStretchTuningsFromFile(context, missingRowUri).fileCheck)
    }

    @Test
    fun filenameOfMissingFileIsUnknown() {
        assertNull(getFilenameFromUri(context, missingRowUri))
    }
}
