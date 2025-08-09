package de.moekadu.tuner.stretchtuning

import androidx.compose.runtime.Immutable
import de.moekadu.tuner.misc.GetText
import de.moekadu.tuner.temperaments.ratioToCents
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.random.Random

private fun DoubleArray.removeAt(position: Int): DoubleArray {
    return DoubleArray(size - 1) { this[if (it < position) it else it + 1] }
}
private fun DoubleArray.replace(i: Int, value: Double): DoubleArray {
    return DoubleArray(size) { if (it == i) value else this[it]}
}
private fun DoubleArray.add(position: Int, value: Double): DoubleArray {
    return DoubleArray(size + 1) {
        if (it < position) this[it]
        else if (it == position) value
        else this[it - 1]
    }
}
private fun IntArray.removeAt(position: Int): IntArray {
    return IntArray(size - 1) { this[if (it < position) it else it + 1] }
}
private fun IntArray.add(position: Int, value: Int): IntArray {
    return IntArray(size + 1) {
        if (it < position) this[it]
        else if (it == position) value
        else this[it - 1]
    }
}

@Immutable
@Serializable
data class StretchTuningData(
    val name: GetText,
    val description: GetText,
    val unstretchedFrequencies: DoubleArray,
    val stretchedFrequencies: DoubleArray,
    val keys: IntArray = IntArray(unstretchedFrequencies.size) { it },
    val stableId: Long = NO_STABLE_ID
) {
    val size get() = unstretchedFrequencies.size

    @Transient
    val cents = unstretchedFrequencies.zip(stretchedFrequencies) { unstretched, stretched ->
        ratioToCents(stretched / unstretched)
    }.toImmutableList()

    fun add(unstretchedFrequency: Double, stretchedFrequency: Double) : StretchTuningData {
        return add(unstretchedFrequency, stretchedFrequency, generateKey())
    }

    fun remove(key: Int): StretchTuningData {
        val position = keys.indexOf(key)
        return if (position >= 0) {
            StretchTuningData(
                name = name,
                description = description,
                unstretchedFrequencies = unstretchedFrequencies.removeAt(position),
                stretchedFrequencies = stretchedFrequencies.removeAt(position),
                keys = keys.removeAt(position)
            )
        } else {
            this
        }
    }

    fun modify(unstretchedFrequency: Double, stretchedFrequency: Double, key: Int) : StretchTuningData {
        val position = keys.binarySearch(key)
        return if (position >= 0 && unstretchedFrequencies[position] == unstretchedFrequency) {
            StretchTuningData(
                name = name,
                description = description,
                unstretchedFrequencies = unstretchedFrequencies,
                stretchedFrequencies = stretchedFrequencies.replace(position, stretchedFrequency),
                keys = keys
            )
        } else if (position >= 0) {
            this.remove(key).add(unstretchedFrequency, stretchedFrequency, key)
        } else {
            this
        }
    }

    private fun add(unstretchedFrequency: Double, stretchedFrequency: Double, key: Int) : StretchTuningData {
        val position = stretchedFrequencies.binarySearch(unstretchedFrequency)
        val positionResolved = if (position >= 0)
            position
        else
            -position - 1
        return StretchTuningData(
            name = name,
            description = description,
            unstretchedFrequencies = unstretchedFrequencies.add(positionResolved, unstretchedFrequency),
            stretchedFrequencies = stretchedFrequencies.add(positionResolved, stretchedFrequency),
            keys = keys.add(positionResolved, key)
        )
    }

    private fun generateKey(): Int {
        var value = Random.nextInt(0, Int.MAX_VALUE - 1)
        while (keys.contains(value))
            value = Random.nextInt(0, Int.MAX_VALUE - 1)
        return value
    }
    companion object {
        const val NO_KEY = Int.MAX_VALUE
        const val NO_STABLE_ID = Long.MAX_VALUE
    }
}