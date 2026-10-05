package de.moekadu.tuner

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.moekadu.tuner.misc.preserveUnreadableValue
import de.moekadu.tuner.misc.unreadableBackupKey
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
import org.junit.Before
import org.junit.Test

class PreserveUnreadableValueTest {
    private val key = stringPreferencesKey("custom instruments")
    private lateinit var directory: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        directory = createTempDirectory().toFile()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            File(directory, "test.preferences_pb")
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
        directory.deleteRecursively()
    }

    @Test
    fun undecodableValueSurvivesOverwriteOfKey() = runBlocking {
        dataStore.edit { it[key] = "undecodable" }

        dataStore.preserveUnreadableValue(key, "undecodable")
        // what a writer does after reading the fallback value
        dataStore.edit { it[key] = "[]" }

        val stored = dataStore.data.first()
        assertEquals("[]", stored[key])
        assertEquals("undecodable", stored[unreadableBackupKey(key)])
    }

    @Test
    fun firstBackupIsKept() = runBlocking {
        dataStore.preserveUnreadableValue(key, "original user data")
        dataStore.preserveUnreadableValue(key, "derived from fallback")

        assertEquals("original user data", dataStore.data.first()[unreadableBackupKey(key)])
    }
}
