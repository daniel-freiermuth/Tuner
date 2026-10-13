package de.moekadu.tuner

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import de.moekadu.tuner.misc.readFloatOrLegacyInt
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ReadFloatOrLegacyIntTest {
    private val key = floatPreferencesKey("overlap")
    private val fromPercent: (Int) -> Float = { it.toFloat() / 100f }
    private lateinit var directory: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        directory = createTempDirectory().toFile()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(directory, "settings.preferences_pb")
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
        directory.deleteRecursively()
    }

    @Test
    fun intPercentOfOlderVersionIsConverted() = runBlocking {
        // v8.1 stored the overlap as int percent under the same name
        dataStore.edit { it[intPreferencesKey("overlap")] = 25 }

        assertEquals(0.25f, dataStore.data.first().readFloatOrLegacyInt(key, fromPercent))
    }

    @Test
    fun storedFloatIsNotConverted() = runBlocking {
        dataStore.edit { it[key] = 0.5f }

        assertEquals(0.5f, dataStore.data.first().readFloatOrLegacyInt(key, fromPercent))
    }

    @Test
    fun intWithoutConversionIsNotUsable() = runBlocking {
        dataStore.edit { it[intPreferencesKey("overlap")] = 25 }

        assertNull(dataStore.data.first().readFloatOrLegacyInt(key, null))
    }
}
