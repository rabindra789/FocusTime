package com.rabindra.focustime.dnd

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.rabindra.focustime.data.DndStateRepository
import com.rabindra.focustime.data.SettingsRepository
import com.rabindra.focustime.util.FocusLog

/**
 * Applies the configured interruption filter for focus sessions and restores the user's
 * filter afterwards, but only while FocusTime still owns the change.
 */
class DndManager(
    private val context: Context,
    private val stateRepository: DndStateRepository,
    private val settingsRepository: SettingsRepository,
) {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    fun hasAccess(): Boolean = runCatching {
        notificationManager.isNotificationPolicyAccessGranted
    }.getOrDefault(false)

    fun accessSettingsIntent(): Intent =
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun currentFilter(): Int? = runCatching { notificationManager.currentInterruptionFilter }.getOrNull()

    suspend fun applyForFocus(): Boolean {
        val settings = settingsRepository.current()
        if (!settings.autoDnd) return false
        if (!hasAccess()) return false
        val current = currentFilter() ?: return false
        val saved = stateRepository.read()
        if (!DndPolicy.shouldApply(saved, current, settings.dndMode.filter)) return false
        val applied = runCatching {
            notificationManager.setInterruptionFilter(settings.dndMode.filter)
        }.isSuccess
        if (!applied) {
            FocusLog.w("DND filter could not be applied")
            return false
        }
        stateRepository.write(previousFilter = current, appliedFilter = settings.dndMode.filter)
        return true
    }

    /** Restores the filter only when the current filter is still the one FocusTime applied. */
    suspend fun releaseIfOwned(): DndRestoreDecision {
        val saved = stateRepository.read()
        if (saved.previousFilter == null && saved.appliedFilter == null) {
            // Defensive cleanup for an implicit rule left behind by a previous process.
            removeOwnedImplicitRule()
            return DndRestoreDecision.NothingToDo
        }
        if (!saved.isOwner) {
            stateRepository.clear()
            removeOwnedImplicitRule()
            return DndRestoreDecision.NothingToDo
        }
        if (!hasAccess()) {
            stateRepository.clear()
            FocusLog.w("DND access revoked; ownership cleared without restoring")
            return DndRestoreDecision.NothingToDo
        }
        val current = currentFilter()
        if (current == null) {
            stateRepository.clear()
            removeOwnedImplicitRule()
            return DndRestoreDecision.NothingToDo
        }
        val decision = DndPolicy.decideOnRelease(saved, current)
        when (decision) {
            is DndRestoreDecision.Restore -> {
                runCatching { notificationManager.setInterruptionFilter(decision.filter) }
                stateRepository.clear()
                if (decision.filter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                    removeOwnedImplicitRule()
                }
            }

            DndRestoreDecision.ClearWithoutRestoring, DndRestoreDecision.NothingToDo -> {
                stateRepository.clear()
                removeOwnedImplicitRule()
            }
        }
        return decision
    }

    /**
     * Android 16 keeps an implicit automatic zen rule per package after setInterruptionFilter().
     * Removing it prevents a stale FocusTime rule from holding Do Not Disturb on.
     */
    private fun removeOwnedImplicitRule() {
        runCatching {
            notificationManager.removeAutomaticZenRule("implicit_${context.packageName}")
        }
    }

    /**
     * Keeps DND aligned with the real session, including a focus session that was restored or
     * auto-started after process death. Re-applying is a no-op while FocusTime already owns it.
     */
    suspend fun reconcile(isFocusActive: Boolean) {
        if (isFocusActive) applyForFocus() else releaseIfOwned()
    }
}
