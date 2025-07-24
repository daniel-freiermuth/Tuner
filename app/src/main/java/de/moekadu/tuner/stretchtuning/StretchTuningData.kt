package de.moekadu.tuner.stretchtuning

import de.moekadu.tuner.temperaments.ratioToCents
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class StretchTuningData(
    val unstretchedFrequencies: ImmutableList<Double>,
    val stretchedFrequencies: ImmutableList<Double>
) {
    val size get() = unstretchedFrequencies.size

    @Transient
    val cents = unstretchedFrequencies.zip(stretchedFrequencies) { unstretched, stretched ->
        ratioToCents(stretched / unstretched)
    }.toImmutableList()
}