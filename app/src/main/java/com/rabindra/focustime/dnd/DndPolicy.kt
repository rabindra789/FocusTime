package com.rabindra.focustime.dnd

/** What FocusTime remembers about the interruption filter it changed. */
data class SavedDndState(
    val previousFilter: Int? = null,
    val appliedFilter: Int? = null,
) {
    val isOwner: Boolean get() = previousFilter != null && appliedFilter != null
}

sealed interface DndRestoreDecision {
    /** FocusTime still owns the current filter: put the previous filter back. */
    data class Restore(val filter: Int) : DndRestoreDecision

    /** The user or another app changed the filter: keep it, forget our saved state. */
    data object ClearWithoutRestoring : DndRestoreDecision

    data object NothingToDo : DndRestoreDecision
}

object DndPolicy {

    fun shouldApply(saved: SavedDndState, currentFilter: Int, desiredFilter: Int): Boolean =
        !saved.isOwner && currentFilter != desiredFilter

    fun decideOnRelease(saved: SavedDndState, currentFilter: Int): DndRestoreDecision {
        val previous = saved.previousFilter ?: return DndRestoreDecision.NothingToDo
        val applied = saved.appliedFilter ?: return DndRestoreDecision.ClearWithoutRestoring
        return if (currentFilter == applied) {
            DndRestoreDecision.Restore(previous)
        } else {
            DndRestoreDecision.ClearWithoutRestoring
        }
    }
}
