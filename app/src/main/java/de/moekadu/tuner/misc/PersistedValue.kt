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
