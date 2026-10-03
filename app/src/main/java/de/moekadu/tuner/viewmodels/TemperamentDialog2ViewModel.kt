package de.moekadu.tuner.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.moekadu.tuner.R
import de.moekadu.tuner.hilt.ApplicationScope
import de.moekadu.tuner.notenames.MusicalNote
import de.moekadu.tuner.temperaments.Temperament3
import de.moekadu.tuner.temperaments.Temperament3Custom
import de.moekadu.tuner.temperaments.TemperamentIO
import de.moekadu.tuner.temperaments.TemperamentResources
import de.moekadu.tuner.ui.common.EditableListData
import de.moekadu.tuner.ui.common.EditableListPredefinedSectionImmutable
import de.moekadu.tuner.ui.temperaments.TemperamentsDialog2Data
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TemperamentDialog2ViewModel @Inject constructor(
    val pref: TemperamentResources,
    @param:ApplicationScope val applicationScope: CoroutineScope
) : ViewModel(),
    TemperamentsDialog2Data {
    private val musicalScale = pref.musicalScale.value

    private val activeTemperament = MutableStateFlow(musicalScale.temperament)

    override val listData = EditableListData(
        predefinedItemSections = persistentListOf(
            pref.edoTemperaments,
            EditableListPredefinedSectionImmutable(
                sectionStringResourceId = R.string.other_temperaments,
                items = pref.predefinedTemperaments,
                isExpanded = pref.predefinedTemperamentsExpanded,
                toggleExpanded = { pref.writePredefinedTemperamentsExpanded(it) }
            )
        ),
        editableItemsSectionResId = R.string.custom_temperaments,
        editableItems = pref.customTemperaments,
        getStableId = { it.stableId },
        editableItemsExpanded = pref.customTemperamentsExpanded,
        activeItem = activeTemperament,
        setNewItems = {
            val newTemperaments = it.filterIsInstance<Temperament3Custom>()
            pref.writeCustomTemperaments(newTemperaments)
        },
        toggleEditableItemsExpanded = { pref.writeCustomTemperamentsExpanded(it) }
    )

    override val defaultTemperament: Temperament3 get() = pref.defaultTemperament

    override fun saveTemperaments(
        context: Context,
        uri: Uri,
        temperaments: List<Temperament3Custom>
    ) {
        applicationScope.launch(Dispatchers.IO) {
            context.contentResolver?.openOutputStream(uri, "wt")?.use { stream ->
                stream.bufferedWriter().use { writer ->
                    TemperamentIO.writeTemperaments(temperaments, writer, context)
                }
            }
        }
    }

    override fun proposeRootNote(temperament: Temperament3): MusicalNote =
        proposeRootNoteFor(temperament, pref.musicalScale.value.rootNote)
}

/** Propose a root note when switching to a new temperament.
 * @param temperament Newly chosen temperament.
 * @param currentRootNote Root note currently in use.
 * @return [currentRootNote] if it is a possible root note of [temperament] (ignoring the
 *   octave), else the first possible root note of [temperament].
 */
internal fun proposeRootNoteFor(
    temperament: Temperament3,
    currentRootNote: MusicalNote
): MusicalNote {
    val possibleRootNotes = temperament.possibleRootNotes()
    val rootNoteInTemperament = possibleRootNotes.any { it.equalsIgnoreOctave(currentRootNote) }
    return if (rootNoteInTemperament) currentRootNote else possibleRootNotes[0]
}
