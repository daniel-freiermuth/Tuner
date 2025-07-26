package de.moekadu.tuner.ui.stretchtuning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.ui.theme.TunerTheme

data class StretchTuningTableLineState(
    val frequencyUnstretched: Double,
    val stretchInCents: Double,
    val key: Long
)

@Composable
fun rememberTableColumnWidth(
    testString: String,
    style: TextStyle = LocalTextStyle.current,
    density: Density = LocalDensity.current,
    textMeasurer: TextMeasurer = rememberTextMeasurer()
): Dp {
    return remember(textMeasurer, density, style) {
        with(density) {
            textMeasurer.measure(testString, style = style, density = density).size.width.toDp()
        }
    }
}

@Composable
fun StretchTuningTableLine(
    state: StretchTuningTableLineState,
    frequencyWidth: Dp,
    centCorrectionWidth: Dp,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current
) {
    Surface(
        color = if (isSelected)
            MaterialTheme.colorScheme.surface
        else
            MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            modifier = modifier
                .width(IntrinsicSize.Max)
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.hertz_1f, state.frequencyUnstretched),
                    modifier = Modifier.width(frequencyWidth),
                    maxLines = 1,
                    style = textStyle,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(32.dp))
                Text(
                    stringResource(R.string.cent_1f, state.stretchInCents),
                    modifier = Modifier.width(centCorrectionWidth),
                    style = textStyle,
                    textAlign = TextAlign.End
                )
            }
            HorizontalDivider()
        }
    }
}

@Preview(widthDp = 400, heightDp = 400, showBackground = true)
@Composable
private fun  StretchTuningTableLinePreview() {
    TunerTheme {
        Surface {
            Box {
                Column {
                    val states = remember {
                        listOf(
                            StretchTuningTableLineState(
                                frequencyUnstretched = 440.0,
                                stretchInCents = 0.0,
                                key = 1L
                            ),
                            StretchTuningTableLineState(
                                frequencyUnstretched = 540.0,
                                stretchInCents = 7.0,
                                key = 2L
                            )
                        )
                    }
                    val style = MaterialTheme.typography.bodyLarge
                    val frequencyWidth = rememberTableColumnWidth(
                        stringResource(R.string.hertz_1f, 10000f),
                        style = style
                    )
                    val centCorrectionWidth = rememberTableColumnWidth(
                        stringResource(R.string.cent_1f, 10f),
                        style = style
                    )

                    states.forEachIndexed { index, state ->
                        StretchTuningTableLine(
                            state = state,
                            isSelected = (index == 0),
                            frequencyWidth = frequencyWidth,
                            centCorrectionWidth = centCorrectionWidth,
                            textStyle = style
                        )
                    }
                }
            }
        }
    }
}