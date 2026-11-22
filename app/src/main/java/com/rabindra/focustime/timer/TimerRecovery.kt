package com.rabindra.focustime.timer

data class RecoveryResult(
    val state: TimerState,
    /** True when a focus session finished while the app was not observing it. */
    val completedFocus: Boolean,
    val completedFocusDurationMillis: Long = 0L,
    /** The session that expired during this reconcile, or null when nothing completed. */
    val completedSession: SessionType? = null,
) {
    /**
     * True when this reconcile also started the following session, which only happens when the
     * user's automatic-flow preference allows it.
     */
    val startedSession: Boolean
        get() = completedSession != null && state.status == TimerStatus.RUNNING
}

/**
 * Reconciles persisted state with the real clock. A session that already expired is completed
 * exactly once, and the following session starts immediately when automatic flow is enabled for
 * it. Auto-starting never invents sessions: at most one transition happens per reconcile, and the
 * next session starts from the moment the completion is detected rather than from its old deadline.
 */
object TimerRecovery {

    fun reconcile(
        persisted: TimerState,
        config: TimerConfig,
        nowWallClock: Long,
        allowAutoStart: Boolean = true,
    ): RecoveryResult {
        val duration = config.durationMillis(persisted.sessionType)
        val normalized = persisted.copy(
            durationMillis = duration,
            remainingMillis = persisted.remainingMillis.coerceIn(0L, duration),
        )
        return when (normalized.status) {
            TimerStatus.IDLE -> RecoveryResult(
                TimerTransitions.initialState(normalized.sessionType, config, nowWallClock).copy(
                    cyclePosition = normalized.cyclePosition,
                    sessionNumber = normalized.sessionNumber,
                ),
                completedFocus = false,
            )

            TimerStatus.PAUSED -> RecoveryResult(
                normalized.copy(updatedAtMillis = nowWallClock),
                completedFocus = false,
            )

            TimerStatus.RUNNING -> {
                if (nowWallClock >= normalized.endWallClockMillis) {
                    RecoveryResult(
                        state = TimerTransitions.advance(
                            normalized,
                            config,
                            nowWallClock,
                            allowAutoStart = allowAutoStart,
                        ),
                        completedFocus = normalized.sessionType.isFocus,
                        completedFocusDurationMillis = if (normalized.sessionType.isFocus) duration else 0L,
                        completedSession = normalized.sessionType,
                    )
                } else {
                    RecoveryResult(normalized.copy(updatedAtMillis = nowWallClock), completedFocus = false)
                }
            }
        }
    }

    /**
     * Wall-clock deadlines drift when the user changes the device time. Re-anchor the persisted
     * deadline to the elapsed-realtime remaining value instead of trusting the old timestamp.
     */
    fun reanchor(state: TimerState, remainingMillis: Long, nowWallClock: Long): TimerState =
        if (state.status != TimerStatus.RUNNING) {
            state
        } else {
            state.copy(
                remainingMillis = remainingMillis.coerceIn(0L, state.durationMillis),
                endWallClockMillis = nowWallClock + remainingMillis.coerceIn(0L, state.durationMillis),
                updatedAtMillis = nowWallClock,
            )
        }
}
