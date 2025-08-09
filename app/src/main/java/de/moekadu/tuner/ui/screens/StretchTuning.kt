package de.moekadu.tuner.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.moekadu.tuner.R
import de.moekadu.tuner.misc.GetTextFromString
import de.moekadu.tuner.stretchtuning.StretchTuningData
import de.moekadu.tuner.temperaments.centsToRatio
import de.moekadu.tuner.ui.misc.TunerScaffoldWithoutBottomBar
import de.moekadu.tuner.ui.plot.GestureBasedViewPort
import de.moekadu.tuner.ui.stretchtuning.StretchTuningGraph
import de.moekadu.tuner.ui.stretchtuning.StretchTuningTable
import de.moekadu.tuner.ui.theme.TunerTheme
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface StretchTuningState {
    val stretchTuningData: StateFlow<StretchTuningData>
    val selectedKey: StateFlow<Int>
    val gestureBasedViewPort: GestureBasedViewPort

    fun select(key: Int)
    fun addLine()
    fun removeLine(key: Int)
    fun modifyLine(unstretchedFrequency: Double, stretchInCents: Double, key: Int)
}

@Composable
private fun OverflowMenu(
    onLoadClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = {
            expanded = !expanded
        }) {
            Icon(Icons.Default.MoreVert, contentDescription = "menu")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.edit)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = "load") },
                onClick = {
                    onLoadClicked()
                    expanded = false
                }
            )
            // HorizontalDivider()
        }
    }
}

@Composable
fun StretchTuning(
    state:  StretchTuningState,
    modifier: Modifier = Modifier,
    onEditLineClicked: ( key: Int ) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            StretchTuningLandscape(state, modifier, onEditLineClicked)
        }

        else -> {
            StretchTuningPortrait(state, modifier, onEditLineClicked)
        }
    }
}

@Composable
private fun StretchTuningPortrait(
    state: StretchTuningState,
    modifier: Modifier = Modifier,
    onEditLineClicked: ( key: Int ) -> Unit = {}
) {
    val stretchTuningData by state.stretchTuningData.collectAsStateWithLifecycle()
    val selectedKey by state.selectedKey.collectAsStateWithLifecycle()

    TunerScaffoldWithoutBottomBar(
        title = stringResource(R.string.stretch_tuning),
        showPreferenceButton = false,
        defaultModeTools = { OverflowMenu(onLoadClicked = {}) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    state.addLine()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier.consumeWindowInsets(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val layoutDirection = LocalLayoutDirection.current

            StretchTuningGraph(
                stretchTuningData,
                selectedKey = selectedKey,
                gestureBasedViewPort = state.gestureBasedViewPort,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        top = 16.dp + paddingValues.calculateTopPadding(),
                        start = 16.dp + paddingValues.calculateStartPadding(layoutDirection),
                        end = 16.dp + paddingValues.calculateEndPadding(layoutDirection),
                        bottom = 8.dp
                    )
            )
            StretchTuningTable(
                stretchTuningData,
                selectedKey = selectedKey,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                contentPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    top = 0.dp,
                    bottom = paddingValues.calculateBottomPadding() + 76.dp // additional padding to get above the fab button
                ),
                onLineClicked = { state.select(it) },
                onEditLineClicked = { onEditLineClicked(it) },
                onDeleteLineClicked = { state.removeLine(it) }
            )
        }
    }
}

@Composable
private fun StretchTuningLandscape(
    state: StretchTuningState,
    modifier: Modifier = Modifier,
    onEditLineClicked: ( key: Int ) -> Unit = {}
) {
    val stretchTuningData by state.stretchTuningData.collectAsStateWithLifecycle()
    val selectedKey by state.selectedKey.collectAsStateWithLifecycle()

    TunerScaffoldWithoutBottomBar(
        title = stringResource(R.string.stretch_tuning),
        showPreferenceButton = false,
        defaultModeTools = { OverflowMenu(onLoadClicked = {}) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    state.addLine()
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "add")
            }
        }
    ) { paddingValues ->
        Row(
            modifier = modifier.consumeWindowInsets(paddingValues)
        ) {
            val layoutDirection = LocalLayoutDirection.current

            StretchTuningGraph(
                stretchTuningData,
                selectedKey = selectedKey,
                gestureBasedViewPort = state.gestureBasedViewPort,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        top = 16.dp + paddingValues.calculateTopPadding(),
                        start = 16.dp + paddingValues.calculateStartPadding(layoutDirection),
                        end = 8.dp,
                        bottom = 8.dp + paddingValues.calculateBottomPadding()
                    )
            )
            StretchTuningTable(
                stretchTuningData,
                selectedKey = selectedKey,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(
                        top = 16.dp + paddingValues.calculateTopPadding(),
                        start = 0.dp,
                        end = 16.dp
                    ),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = paddingValues.calculateEndPadding(layoutDirection),
                    top = 0.dp,
                    bottom = paddingValues.calculateBottomPadding() + 76.dp // additional padding to get above the fab button
                ),
                onLineClicked = { state.select(it) },
                onEditLineClicked = { onEditLineClicked(it) },
                onDeleteLineClicked = { state.removeLine(it) }
            )
        }
    }
}

private class TestStretchTuningState : StretchTuningState {
    override val stretchTuningData = MutableStateFlow(StretchTuningData(
        name = GetTextFromString("Test"),
        description = GetTextFromString("Stretch tuning description."),
        unstretchedFrequencies = doubleArrayOf(320.0, 440.0, 540.0, 1023.4, 3000.0, 12310.0),
        stretchedFrequencies = doubleArrayOf(318.0, 440.0, 541.0, 1025.0, 3010.0, 12330.0)
    ))
    override val selectedKey = MutableStateFlow(2)

    override val gestureBasedViewPort = GestureBasedViewPort()

    override fun select(key: Int) {
        selectedKey.value = key
    }

    override fun addLine() {
        stretchTuningData.value = stretchTuningData.value.add(
            stretchTuningData.value.unstretchedFrequencies[0] - 10,
            stretchTuningData.value.unstretchedFrequencies[0] - 11
        )
    }

    override fun removeLine(key: Int) {
        stretchTuningData.value = stretchTuningData.value.remove(key)
    }

    override fun modifyLine(unstretchedFrequency: Double, stretchInCents: Double, key: Int) {
        stretchTuningData.value = stretchTuningData.value.modify(
            unstretchedFrequency,
            unstretchedFrequency * centsToRatio(stretchInCents),
            key
        )
    }
}

@Preview(widthDp = 400, heightDp = 800)
@Preview(widthDp = 800, heightDp = 400)
@Composable
fun StretchTuningPreview() {
    TunerTheme {
        val state = remember { TestStretchTuningState() }

        Surface() {
            StretchTuning(state,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}