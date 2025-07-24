package de.moekadu.tuner.ui.stretchtuning

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DoubleState
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.ui.theme.TunerTheme

class StretchTuningTableLineState(
    initialFrequencyUnstretched: Double,
    initialStretchInCents: Double,
    val key: Long
) {
    private val _frequencyUnstretched = mutableDoubleStateOf(initialFrequencyUnstretched)
    val frequencyUnstretched: DoubleState get() = _frequencyUnstretched

    private val _stretchInCents = mutableDoubleStateOf(initialStretchInCents)
    val stretchInCents: DoubleState get() = _stretchInCents
}

@Composable
fun StretchTuningTableLine(
    state: StretchTuningTableLineState,
    modifier: Modifier = Modifier
) {
    Row(modifier) {
        Text(
            state.frequencyUnstretched.doubleValue.toString(),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Text(state.stretchInCents.doubleValue.toString())

    }
}

@Preview(widthDp = 400, heightDp = 400, showBackground = true)
@Composable
private fun  StretchTuningTableLinePreview() {
    TunerTheme {
        Surface { Column {
            val states = remember {
                listOf(
                    StretchTuningTableLineState(
                        initialFrequencyUnstretched = 440.0,
                        initialStretchInCents = 0.0,
                        key = 1L
                    ),
                    StretchTuningTableLineState(
                        initialFrequencyUnstretched = 540.0,
                        initialStretchInCents = 7.0,
                        key = 2L
                    )
                )
            }

            states.forEach {
                StretchTuningTableLine(
                    it,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } }
    }
}