package de.moekadu.tuner.ui.stretchtuning

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import de.moekadu.tuner.R
import de.moekadu.tuner.stretchtuning.StretchTuningData
import de.moekadu.tuner.temperaments.centsToFrequency
import de.moekadu.tuner.temperaments.ratioToCents
import de.moekadu.tuner.ui.plot.Anchor
import de.moekadu.tuner.ui.plot.HorizontalLinesPositions
import de.moekadu.tuner.ui.plot.LineCoordinates
import de.moekadu.tuner.ui.plot.Plot
import de.moekadu.tuner.ui.plot.PlotWindowOutline
import de.moekadu.tuner.ui.plot.TickLevelDeltaBased
import de.moekadu.tuner.ui.plot.rememberTextLabelHeight
import de.moekadu.tuner.ui.plot.rememberTextLabelWidth
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

@Composable
fun StretchTuningGraph(
    stretchTuningData: StretchTuningData,
    modifier: Modifier = Modifier,
    // line properties
    lineWidth: Dp = 2.dp,
    lineColor: Color = MaterialTheme.colorScheme.onSurface,
    // tick properties
    tickLineWidth: Dp = 1.dp,
    tickLineColor: Color = MaterialTheme.colorScheme.outline,
    tickLabelStyle: TextStyle = MaterialTheme.typography.labelMedium,
    tickLabelColor: Color = MaterialTheme.colorScheme.onSurface,
    // outline
    plotWindowOutline: PlotWindowOutline = PlotWindowOutline(),
    // frequencies
    referenceFrequency: Float = 440f
) {
    val viewPort = remember(stretchTuningData, referenceFrequency) {
        val fmin = stretchTuningData.unstretchedFrequencies.min()
        val fmax = stretchTuningData.unstretchedFrequencies.max()
        val cmin = stretchTuningData.cents.min()
        val cmax = stretchTuningData.cents.max()
        Rect(
            left = ratioToCents(fmin / referenceFrequency.toDouble()).toFloat(),
            top = cmax.toFloat(),
            right = ratioToCents(fmax / referenceFrequency.toDouble()).toFloat(),
            bottom = cmin.toFloat()
        )
    }
    val maxLabelWidth = rememberTextLabelWidth(
        stringResource(R.string.cent, 500),
        style = tickLabelStyle
    )
    val maxLabelWidthDp = with(LocalDensity.current) { maxLabelWidth.toDp() + 2.dp}

    val maxLabelHeight = rememberTextLabelHeight(tickLabelStyle)
    val maxLabelHeightDp = with(LocalDensity.current) { maxLabelHeight.toDp() + 2.dp}

    Plot(
        modifier = modifier,
        viewPort = viewPort,
        plotWindowPadding = DpRect(
            left = 0.dp,
            top = 0.dp,
            right = maxLabelWidthDp,
            bottom = maxLabelHeightDp
        ),
        plotWindowOutline = plotWindowOutline
    ) {
        val horizontalLinePositions = remember {
            HorizontalLinesPositions.create(floatArrayOf(0f))
        }
        val line = remember(stretchTuningData, referenceFrequency) {
            LineCoordinates.create(
                stretchTuningData.size,
                { ratioToCents(stretchTuningData.unstretchedFrequencies[it] / referenceFrequency.toDouble()).toFloat() },
                { stretchTuningData.cents[it].toFloat() }
            )
        }
        val tickLevelX = remember {
            TickLevelDeltaBased(600f, 1200f, 2400f)
        }

        val tickLevelY = remember {
            TickLevelDeltaBased(1f, 2f, 3f, 5f, 10f, 20f, 50f, 100f, 200f, 500f)
        }
        XTicks(
            tickLevelX,
            maxLabelWidth = rememberTextLabelWidth(
                stringResource(R.string.hertz, 10000f),
                tickLabelStyle
            ),
            verticalLabelPosition = 0f,
            anchor = Anchor.North,
            lineWidth = tickLineWidth,
            lineColor = tickLineColor,
            clipLabelToPlotWindow = false
        ) { tickModifier, _, _, c ->
            Text(
                stringResource(R.string.hertz, centsToFrequency(c.toDouble(), referenceFrequency.toDouble()).toFloat()),
                modifier = tickModifier.padding(horizontal = 2.dp),
                style = tickLabelStyle,
                color = tickLabelColor
            )
        }
        YTicks(
            tickLevelY,
            maxLabelHeight = maxLabelHeight,
            horizontalLabelPosition = 1f,
            anchor = Anchor.West,
            lineWidth = tickLineWidth,
            lineColor = tickLineColor,
            clipLabelToPlotWindow = false
        ) { tickModifier, _, _, c ->
            Text(
                stringResource(R.string.cent, c.roundToInt()),
                modifier = tickModifier.padding(horizontal = 2.dp),
                style = tickLabelStyle,
                color = tickLabelColor
            )
        }
        HorizontalLines(
            positions = horizontalLinePositions
        )
        Line(
            data = line,
            lineColor = lineColor,
            lineWidth = lineWidth
        )
    }
}

@Preview(widthDp = 400, heightDp = 300, showBackground = true)
@Composable
private fun StretchTuningGraphPreview() {
    TunerTheme {
        Surface {
            val stretchTuningData = remember {StretchTuningData(
                persistentListOf(100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0, 1200.0),
                persistentListOf( 97.0, 199.0, 297.0, 400.0, 502.0, 606.0, 708.0, 807.0, 913.0, 1220.0)
            )}
            StretchTuningGraph(
                stretchTuningData = stretchTuningData,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        }
    }
}