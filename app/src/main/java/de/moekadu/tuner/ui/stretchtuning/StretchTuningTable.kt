package de.moekadu.tuner.ui.stretchtuning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.max
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.GetTextFromString
import de.moekadu.tuner.stretchtuning.StretchTuningData
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.toPersistentList

@Composable
fun StretchTuningTable(
    stretchTuningData: StretchTuningData,
    selectedKey: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onLineClicked: (key: Int) -> Unit = {},
    onEditLineClicked: (key: Int) -> Unit = {},
    onDeleteLineClicked: (key: Int) -> Unit = {}
) {
    val bodyStyle = MaterialTheme.typography.bodyLarge
    val headlineStyle = MaterialTheme.typography.labelLarge

    val minimumFrequencyColumnWidth = rememberTableColumnWidth(
        stringResource(R.string.hertz_1f, 10000.0),
        style = bodyStyle
    )
    val minimumFrequencyHeadlineWidth = rememberTableColumnWidth(
        stringResource(R.string.frequency),
        style = headlineStyle
    )

    val minimumCentsColumnWidth = rememberTableColumnWidth(
        stringResource(R.string.cent_1f, 100.0),
        style = bodyStyle
    )
    val minimumCentHeadlineWidth = rememberTableColumnWidth(
        stringResource(R.string.stretch),
        style = headlineStyle
    )

    Column(modifier = modifier) {

        StretchTuningTableLine(
            column1 = stringResource(R.string.frequency),
            column2 = stringResource(R.string.stretch),
            column1Width = max(minimumFrequencyColumnWidth, minimumFrequencyHeadlineWidth),
            column2Width = max(minimumCentsColumnWidth, minimumCentHeadlineWidth),
            isSelected = false,
            textStyle = headlineStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            showOverflowMenu = false
        )

        LazyColumn(
            contentPadding = contentPadding
        ) {
            items(
                stretchTuningData.size,
                key = { stretchTuningData.keys[it] },
                contentType = { 1 }) {
                StretchTuningTableLine(
                    column1 = stringResource(
                        R.string.hertz_1f,
                        stretchTuningData.unstretchedFrequencies[it]
                    ),
                    column2 = stringResource(R.string.cent_1f, stretchTuningData.cents[it]),
                    column1Width = max(minimumFrequencyColumnWidth, minimumFrequencyHeadlineWidth),
                    column2Width = max(minimumCentsColumnWidth, minimumCentHeadlineWidth),
                    isSelected = stretchTuningData.keys[it] == selectedKey,
                    textStyle = bodyStyle,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .clickable { onLineClicked(stretchTuningData.keys[it]) },
                    onEditClicked = { onEditLineClicked(stretchTuningData.keys[it]) },
                    onDeleteClicked = { onDeleteLineClicked(stretchTuningData.keys[it]) }
                )
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 300, showBackground = true)
@Composable
private fun StretchTuningTablePreview() {
    TunerTheme {
        val data = StretchTuningData(
            name = GetTextFromString("Test"),
            description = GetTextFromString("Stretch tuning description."),
            unstretchedFrequencies = doubleArrayOf(320.0, 440.0, 540.0, 1023.4, 3000.0, 12310.0),
            stretchedFrequencies = doubleArrayOf(318.0, 440.0, 541.0, 1025.0, 3010.0, 12330.0)
        )
        var selectedKey by remember { mutableIntStateOf(4) }
        Box {
            StretchTuningTable(
                data,
                selectedKey = selectedKey,
                onLineClicked = { selectedKey = it }
            )
        }
    }
}