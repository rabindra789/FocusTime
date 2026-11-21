package com.rabindra.focustime.dnd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DndPolicyTest {

    private val all = 1
    private val priority = 2
    private val alarms = 4

    @Test
    fun `applies the filter when the user is not already in that mode`() {
        assertTrue(DndPolicy.shouldApply(SavedDndState(), currentFilter = all, desiredFilter = priority))
    }

    @Test
    fun `does not touch the filter when it already matches the desired mode`() {
        assertFalse(DndPolicy.shouldApply(SavedDndState(), currentFilter = priority, desiredFilter = priority))
    }

    @Test
    fun `never re-captures a filter that FocusTime already owns`() {
        val saved = SavedDndState(previousFilter = all, appliedFilter = priority)
        assertFalse(DndPolicy.shouldApply(saved, currentFilter = priority, desiredFilter = alarms))
    }

    @Test
    fun `restores the previous filter when FocusTime still owns the change`() {
        val saved = SavedDndState(previousFilter = all, appliedFilter = priority)
        assertEquals(
            DndRestoreDecision.Restore(all),
            DndPolicy.decideOnRelease(saved, currentFilter = priority),
        )
    }

    @Test
    fun `keeps a newer manual preference instead of overwriting it`() {
        val saved = SavedDndState(previousFilter = all, appliedFilter = priority)
        assertEquals(
            DndRestoreDecision.ClearWithoutRestoring,
            DndPolicy.decideOnRelease(saved, currentFilter = alarms),
        )
    }

    @Test
    fun `keeps a manually disabled DND when FocusTime had applied priority`() {
        val saved = SavedDndState(previousFilter = all, appliedFilter = priority)
        assertEquals(
            DndRestoreDecision.ClearWithoutRestoring,
            DndPolicy.decideOnRelease(saved, currentFilter = all),
        )
    }

    @Test
    fun `nothing happens when no state was saved`() {
        assertEquals(
            DndRestoreDecision.NothingToDo,
            DndPolicy.decideOnRelease(SavedDndState(), currentFilter = priority),
        )
    }

    @Test
    fun `a partially saved state is cleared without restoring`() {
        val partial = SavedDndState(previousFilter = all, appliedFilter = null)
        assertEquals(
            DndRestoreDecision.ClearWithoutRestoring,
            DndPolicy.decideOnRelease(partial, currentFilter = all),
        )
    }

    @Test
    fun `ownership requires both stored filters`() {
        assertTrue(SavedDndState(previousFilter = all, appliedFilter = priority).isOwner)
        assertFalse(SavedDndState(previousFilter = all).isOwner)
    }
}
