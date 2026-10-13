/*
* Copyright 2026 Michael Moessner
*
* This file is part of Tuner.
*
* Tuner is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Tuner is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Tuner.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.moekadu.tuner.misc

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

/** Read a persisted value, falling back if it cannot be read.
 *
 * Stored values may be corrupt or written by an older app version. Decoding them can fail with
 * more than SerializationException (e.g. ArithmeticException from a zero denominator in
 * RationalNumber, or index errors while converting a legacy format), and none of these may crash
 * the app on start. So this is the one place that catches every Exception; the failure is logged.
 *
 * @param read Decodes the stored value.
 * @param fallback Value to use if [read] throws.
 * @return Result of [read], or of [fallback] if [read] threw.
 */
@Suppress("TooGenericExceptionCaught")
inline fun <T> readPersistedOrElse(read: () -> T, fallback: () -> T): T = try {
    read()
} catch (ex: Exception) {
    Log.w("Tuner", "Cannot read persisted value, using fallback", ex)
    fallback()
}

/** Key under which an undecodable value of [key] is kept. */
fun unreadableBackupKey(key: Preferences.Key<String>) =
    stringPreferencesKey("${key.name} unreadable backup")

/** Store [raw] under [unreadableBackupKey] of [key], unless a backup already exists.
 *
 * An existing backup is kept, since values written after the first decoding failure are derived
 * from the fallback and not from the original user data.
 *
 * @param key Key of the value which could not be decoded.
 * @param raw The stored, undecodable value.
 */
suspend fun DataStore<Preferences>.preserveUnreadableValue(
    key: Preferences.Key<String>,
    raw: String
) {
    val backupKey = unreadableBackupKey(key)
    edit { if (it[backupKey] == null) it[backupKey] = raw }
}

/** Read a float value, which older app versions might have stored as an int under the same name.
 *
 * Keys of the preferences data store are compared by name only, so reading an int with a float
 * key would fail with a ClassCastException.
 *
 * @param key Key of the value.
 * @param fromLegacyInt Conversion of a stored int to the float value, or null if ints are not
 *   expected for this key.
 * @return Stored float, converted stored int, or null if there is no usable value.
 */
fun Preferences.readFloatOrLegacyInt(
    key: Preferences.Key<Float>,
    fromLegacyInt: ((Int) -> Float)?
): Float? = when (val value = asMap()[key]) {
    is Float -> value
    is Int -> fromLegacyInt?.invoke(value)
    else -> null
}
