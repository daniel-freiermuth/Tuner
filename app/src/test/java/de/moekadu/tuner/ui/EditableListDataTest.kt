package de.moekadu.tuner.ui

import de.moekadu.tuner.ui.common.EditableListData
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class EditableListDataTest {
    private data class Item(val key: Long)

    private val a = Item(1L)
    private val b = Item(2L)
    private val c = Item(3L)
    private val d = Item(4L)

    private fun createListData(vararg items: Item): EditableListData<Item> {
        val editableItems = MutableStateFlow<PersistentList<Item>>(persistentListOf(*items))
        return EditableListData(
            predefinedItemSections = persistentListOf(),
            editableItemsSectionResId = 0,
            editableItems = editableItems,
            editableItemsExpanded = MutableStateFlow(true),
            toggleEditableItemsExpanded = { },
            getStableId = { it.key },
            activeItem = MutableStateFlow(null),
            setNewItems = { editableItems.value = it }
        )
    }

    private fun EditableListData<Item>.delete(item: Item): EditableListData.ItemsDeletedInfo<Item> {
        deleteItems(persistentSetOf(item.key))
        return editableItemsBackup.tryReceive().getOrThrow()
    }

    @Test
    fun undoingEarlierDeletionKeepsLaterDeletion() {
        val listData = createListData(a, b, c)
        val deletedA = listData.delete(a)
        val deletedB = listData.delete(b)
        assertEquals(listOf(c), listData.editableItems.value)

        listData.undoDeletion(deletedA)
        assertEquals(listOf(a, c), listData.editableItems.value)

        listData.undoDeletion(deletedB)
        assertEquals(listOf(a, b, c), listData.editableItems.value)
    }

    @Test
    fun undoingLaterDeletionKeepsEarlierDeletion() {
        val listData = createListData(a, b, c)
        val deletedC = listData.delete(c)
        val deletedB = listData.delete(b)

        listData.undoDeletion(deletedB)
        assertEquals(listOf(a, b), listData.editableItems.value)

        listData.undoDeletion(deletedC)
        assertEquals(listOf(a, b, c), listData.editableItems.value)
    }

    @Test
    fun undoingDeletionKeepsItemsAddedAfterwards() {
        val listData = createListData(a, b, c)
        val deletedB = listData.delete(b)
        listData.setNewItems(listData.editableItems.value.adding(d))

        listData.undoDeletion(deletedB)
        assertEquals(listOf(a, b, c, d), listData.editableItems.value)
    }

    @Test
    fun undoingDeletionOfAllItemsRestoresThemInOrder() {
        val listData = createListData(a, b, c)
        listData.deleteAllItems()
        val deletedAll = listData.editableItemsBackup.tryReceive().getOrThrow()
        assertEquals(3, deletedAll.numDeleted)

        listData.undoDeletion(deletedAll)
        assertEquals(listOf(a, b, c), listData.editableItems.value)
    }
}
