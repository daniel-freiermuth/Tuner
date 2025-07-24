package de.moekadu.tuner.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.tooling.preview.Preview
import de.moekadu.tuner.ui.plot.Plot
import de.moekadu.tuner.ui.theme.TunerTheme

interface StretchTuningState {

}

@Composable
fun StretchTuning(
    state: StretchTuningState,
    modifier: Modifier = Modifier
) {
    val viewPort = remember {
         Rect(
            left = 0f,
            top = 1f,
            right = 10f,
            bottom = 0f
        )
    }
    Column {
        Plot(
            modifier = Modifier.weight(0.5f),
            viewPort = viewPort
        ) { }
    }
}

private class TestStretchTuningState : StretchTuningState {

}

@Preview(widthDp = 400, heightDp = 800)
@Composable
fun StretchTuningPreview() {
    TunerTheme {
        val state = remember { TestStretchTuningState() }

        Box {
            StretchTuning(state,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}