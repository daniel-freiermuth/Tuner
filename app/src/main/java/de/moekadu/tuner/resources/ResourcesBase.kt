package de.moekadu.tuner.resources

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlin.collections.forEach
import kotlin.let
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

open class ResourcesBase(
    private val filename: String,
    context: Context,
    private val applicationScope: CoroutineScope
) {
    protected interface ResourcePreference {
        fun storeIn(mutableDataStorePreferences: MutablePreferences)
        fun reset()
    }
    protected class SerializableResourcePreference<T : Any?, R : Any?>(
        key: String,
        val default: T,
        prefsForInitialization: Preferences,
        private val serializer: KSerializer<R>,
        private val makeSerializable: (T) -> R,
        private val fromSerializable: (R) -> T,
        private val verifyAfterReading: (T?) -> T?,
        private val onDeserializationFailure: (serialized: String) -> T?,
        private val onChanged: () -> Unit
    ) : ResourcePreference {
        val key = stringPreferencesKey(key)
        private val state = MutableStateFlow(readFromPrefs(prefsForInitialization) ?: default)
        fun asStateFlow() = state.asStateFlow()

        fun set(value: T) {
            state.value = value
            onChanged()
        }

        override fun storeIn(mutableDataStorePreferences: MutablePreferences) {
            mutableDataStorePreferences[key] = Json.encodeToString(
                serializer,
                makeSerializable(state.value)
            )
        }

        override fun reset() {
            set(default)
        }

        private fun readFromPrefs(prefs: Preferences): T? = try {
            prefs[key]?.let { serialized ->
                try {
                    val deserialized = Json.decodeFromString(serializer, serialized)
                    val r = fromSerializable(deserialized)
                    verifyAfterReading(r)
                } catch (_: IllegalArgumentException) {
                    onDeserializationFailure(serialized)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: ClassCastException) {
            null
        }
    }

//    protected class SerializableResourcePreference<T: Any?>(
//        key: String,
//        val default: T,
//        prefsForInitialization: Preferences,
//        private val serializer: KSerializer<T>,
//        private val onChanged: () -> Unit
//    ) : ResourcePreference {
//        val key = stringPreferencesKey(key)
//        private val state = MutableStateFlow(readFromPrefs(prefsForInitialization) ?: default)
//        fun asStateFlow() = state.asStateFlow()
//
//        fun set(value: T) {
//            state.value = value
//            onChanged()
//        }
//
//        override fun storeIn(mutableDataStorePreferences: MutablePreferences) {
//            mutableDataStorePreferences[key] = Json.encodeToString(serializer, state.value)
//        }
//
//        override fun reset() {
//            set(default)
//        }
//
//        private fun readFromPrefs(prefs: Preferences): T? {
//            return try {
//                prefs[key]?.let {
//                    try {
//                        Json.decodeFromString(serializer, it)
//                    } catch (_: Exception) { null }
//                }
//            } catch (ex: ClassCastException) {
//                null
//            }
//        }
//    }

    protected class BoolResourcePreference(
        key: String,
        val default: Boolean,
        prefsForInitialization: Preferences,
        private val onChanged: () -> Unit
    ) : ResourcePreference {
        val key = booleanPreferencesKey(key)
        private val state = MutableStateFlow(
            readFromPrefs(prefsForInitialization) ?: default
        )

        fun asStateFlow() = state.asStateFlow()

        fun set(value: Boolean) {
            state.value = value
            onChanged()
        }

        override fun storeIn(mutableDataStorePreferences: MutablePreferences) {
            mutableDataStorePreferences[key] = state.value
        }

        override fun reset() {
            set(default)
        }

        private fun readFromPrefs(prefs: Preferences): Boolean? = try {
            prefs[key]
        } catch (_: ClassCastException) {
            null
        }
    }

    protected class IntResourcePreference(
        key: String,
        val default: Int,
        prefsForInitialization: Preferences,
        val onChanged: () -> Unit
    ) : ResourcePreference {
        val key = intPreferencesKey(key)
        private val state = MutableStateFlow(readFromPrefs(prefsForInitialization) ?: default)

        fun asStateFlow() = state.asStateFlow()

        fun set(value: Int) {
            state.value = value
            onChanged()
        }
        override fun storeIn(mutableDataStorePreferences: MutablePreferences) {
            mutableDataStorePreferences[key] = state.value
        }

        override fun reset() {
            set(default)
        }
        private fun readFromPrefs(prefs: Preferences): Int? = try {
            prefs[key]
        } catch (_: ClassCastException) {
            null
        }
    }

    protected class FloatResourcePreference(
        key: String,
        val default: Float,
        prefsForInitialization: Preferences,
        val onChanged: () -> Unit
    ) : ResourcePreference {
        val key = floatPreferencesKey(key)
        private val state = MutableStateFlow(readFromPrefs(prefsForInitialization) ?: default)

        fun asStateFlow() = state.asStateFlow()

        fun set(value: Float) {
            state.value = value
            onChanged()
        }
        override fun storeIn(mutableDataStorePreferences: MutablePreferences) {
            mutableDataStorePreferences[key] = state.value
        }

        override fun reset() {
            set(default)
        }
        private fun readFromPrefs(prefs: Preferences): Float? = try {
            prefs[key]
        } catch (_: ClassCastException) {
            null
        }
    }

    protected class StringResourcePreference(
        key: String,
        val default: String,
        prefsForInitialization: Preferences,
        val onChanged: () -> Unit
    ) : ResourcePreference {
        val key = stringPreferencesKey(key)
        private val state = MutableStateFlow(readFromPrefs(prefsForInitialization) ?: default)

        fun asStateFlow() = state.asStateFlow()
        fun set(value: String) {
            state.value = value
            onChanged()
        }

        override fun storeIn(mutableDataStorePreferences: MutablePreferences) {
            mutableDataStorePreferences[key] = state.value
        }

        override fun reset() {
            set(default)
        }
        private fun readFromPrefs(prefs: Preferences): String? = try {
            prefs[key]
        } catch (_: ClassCastException) {
            null
        }
    }

    protected val dataStore = PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
    ) { context.preferencesDataStoreFile(filename) }

    protected val collectedPreferences = mutableListOf<ResourcePreference>()

    protected val newPreferencesAvailableChannel = Channel<Boolean>(Channel.CONFLATED)

    init {
        // store new settings regularly to datastore, but not too often.
        // at app exit, better call once store(), to make sure, everything is really stored.
        val minimumStoreIntervalInMillis = 500L
        applicationScope.launch(Dispatchers.IO) {
            for (value in newPreferencesAvailableChannel) {
                storeCurrentPrefs()
                delay(minimumStoreIntervalInMillis.milliseconds)
            }
        }
    }

    suspend fun storeCurrentPrefs() {
//        Log.v("Metronome", "Storing current prefs")
        dataStore.edit { mutableDataStore ->
            collectedPreferences.forEach { it.storeIn(mutableDataStore) }
        }
    }

    fun store() {
        if (newPreferencesAvailableChannel.tryReceive().isSuccess) {
            applicationScope.launch(Dispatchers.IO) { storeCurrentPrefs() }
        }
    }

    /** Manually invalidate to make this trigger the persist step.
     * This can be used, if you have to change preference internals without changing
     * the preference itself.
     * If you change the preferences with the "set()" method, you do not have to call this,
     * but it will done automatically.
     */
    fun invalidate() {
        newPreferencesAvailableChannel.trySend(true)
    }

    fun resetAllPreferences() {
        collectedPreferences.forEach { it.reset() }
    }

    protected inline fun <reified T : Any?> createSerializablePreference(
        key: String,
        default: T,
        noinline verifyAfterReading: (T?) -> T? = { it },
        noinline onDeserializationFailure: (serialized: String) -> T? = { default }
    ): SerializableResourcePreference<T, T> {
//        Log.v("Metronome", "ResourcesBase.createSerializablePreference: key=$key, default=$default, type=${T::class.simpleName}")
        val availablePrefs = runBlocking { dataStore.data.first() }
        val pref = SerializableResourcePreference(
            key,
            default,
            availablePrefs,
            serializer<T>(),
            { it },
            { it },
            verifyAfterReading,
            onDeserializationFailure
        ) {
            newPreferencesAvailableChannel.trySend(true)
        }
        collectedPreferences.add(pref)
        return pref
    }
    protected inline fun <reified T : Any?, reified R : Any?> createTransformablePreference(
        key: String,
        default: T,
        noinline makeSerializable: (T) -> R,
        noinline fromSerializable: (R) -> T,
        noinline verifyAfterReading: (T?) -> T? = { it },
        noinline onDeserializationFailure: (serialized: String) -> T? = { default }
    ): SerializableResourcePreference<T, R> {
//        Log.v("Metronome", "ResourcesBase.createSerializablePreference: key=$key, default=$default, type=${T::class.simpleName}")
        val availablePrefs = runBlocking { dataStore.data.first() }
        val pref = SerializableResourcePreference(
            key,
            default,
            availablePrefs,
            serializer<R>(),
            makeSerializable,
            fromSerializable,
            verifyAfterReading,
            onDeserializationFailure
        ) {
            newPreferencesAvailableChannel.trySend(true)
        }
        collectedPreferences.add(pref)
        return pref
    }

    protected fun createPreference(key: String, default: Boolean): BoolResourcePreference {
        val availablePrefs = runBlocking { dataStore.data.first() }
        val pref = BoolResourcePreference(key, default, availablePrefs) {
            newPreferencesAvailableChannel.trySend(true)
        }
        collectedPreferences.add(pref)
        return pref
    }

    protected fun createPreference(key: String, default: Int): IntResourcePreference {
        val availablePrefs = runBlocking { dataStore.data.first() }
        val pref = IntResourcePreference(key, default, availablePrefs) {
            newPreferencesAvailableChannel.trySend(true)
        }
        collectedPreferences.add(pref)
        return pref
    }

    protected fun createPreference(key: String, default: Float): FloatResourcePreference {
        val availablePrefs = runBlocking { dataStore.data.first() }
        val pref = FloatResourcePreference(key, default, availablePrefs) {
            newPreferencesAvailableChannel.trySend(true)
        }
        collectedPreferences.add(pref)
        return pref
    }
    protected fun createPreference(key: String, default: String): StringResourcePreference {
        val availablePrefs = runBlocking { dataStore.data.first() }
        val pref = StringResourcePreference(key, default, availablePrefs) {
            newPreferencesAvailableChannel.trySend(true)
        }
        collectedPreferences.add(pref)
        return pref
    }
}
