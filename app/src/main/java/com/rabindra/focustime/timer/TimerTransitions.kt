package com.rabindra.focustime.timer

/**
 * Pure timer state machine. Every function is deterministic and takes the current wall-clock
 * time, which makes the Pomodoro lifecycle fully unit-testable.
 */
object TimerTransitions {

    fun initialState(type: SessionType, config: TimerConfig, nowWallClock: Long = 0L): TimerState {
        val duration = config.durationMillis(type)
        return TimerState(
            sessionType = type,
            status = TimerStatus.IDLE,
            durationMillis = duration,
            remainingMillis = duration,
            updatedAtMillis = nowWallClock,
        )
    }

    fun start(state: TimerState, config: TimerConfig, nowWallClock: Long): TimerState {
        if (state.status == TimerStatus.RUNNING) return state
        val duration = config.durationMillis(state.sessionType)
        val remaining = when (state.status) {
            TimerStatus.PAUSED -> state.remainingMillis.coerceIn(1L, duration)
            TimerStatus.IDLE -> duration
            TimerStatus.RUNNING -> return state
        }
        return state.copy(
            status = TimerStatus.RUNNING,
            durationMillis = duration,
            remainingMillis = remaining,
            endWallClockMillis = nowWallClock + remaining,
            updatedAtMillis = nowWallClock,
        )
    }

    fun pause(state: TimerState, nowWallClock: Long): TimerState {
        if (state.status != TimerStatus.RUNNING) return state
        val remaining = remainingAt(state, nowWallClock)
        return state.copy(
            status = TimerStatus.PAUSED,
            remainingMillis = remaining,
            endWallClockMillis = 0L,
            updatedAtMillis = nowWallClock,
        )
    }

    /**
     * Returns the current session to its configured start with no elapsed time. Resetting a focus
     * session also starts a fresh cycle (session 01, cycle 0) so a reset can never leave the next
     * session carrying the previous run's numbering; a break reset keeps the cycle it belongs to.
     */
    fun reset(state: TimerState, config: TimerConfig, nowWallClock: Long): TimerState {
        val fresh = initialState(state.sessionType, config, nowWallClock)
        return if (state.sessionType.isFocus) {
            fresh
        } else {
            fresh.copy(
                cyclePosition = state.cyclePosition,
                sessionNumber = state.sessionNumber,
            )
        }
    }

    fun remainingAt(state: TimerState, nowWallClock: Long): Long = when (state.status) {
        TimerStatus.RUNNING -> (state.endWallClockMillis - nowWallClock).coerceIn(0L, state.durationMillis)
        TimerStatus.IDLE, TimerStatus.PAUSED -> state.remainingMillis.coerceIn(0L, state.durationMillis)
    }

    fun isFinished(state: TimerState, nowWallClock: Long): Boolean =
        state.status == TimerStatus.RUNNING && nowWallClock >= state.endWallClockMillis

    /** Session that follows [state], plus the cycle position that session starts with. */
    fun nextSession(state: TimerState, config: TimerConfig): Pair<SessionType, Int> =
        when (state.sessionType) {
            SessionType.FOCUS -> {
                val completedFocus = state.cyclePosition + 1
                if (completedFocus >= config.longBreakInterval) {
                    SessionType.LONG_BREAK to 0
                } else {
                    SessionType.SHORT_BREAK to completedFocus
                }
            }

            SessionType.SHORT_BREAK -> SessionType.FOCUS to state.cyclePosition
            SessionType.LONG_BREAK -> SessionType.FOCUS to 0
        }

    /**
     * Moves past the current session. Auto-start is honoured unless [allowAutoStart] is false,
     * which is what recovery after process death or reboot uses.
     */
    fun advance(
        state: TimerState,
        config: TimerConfig,
        nowWallClock: Long,
        allowAutoStart: Boolean = true,
    ): TimerState {
        val (nextType, nextCycle) = nextSession(state, config)
        val duration = config.durationMillis(nextType)
        val autoStart = allowAutoStart &&
            if (nextType == SessionType.FOCUS) config.autoStartFocus else config.autoStartBreaks
        return TimerState(
            sessionType = nextType,
            status = if (autoStart) TimerStatus.RUNNING else TimerStatus.IDLE,
            durationMillis = duration,
            remainingMillis = duration,
            endWallClockMillis = if (autoStart) nowWallClock + duration else 0L,
            cyclePosition = nextCycle,
            sessionNumber = if (nextType == SessionType.FOCUS) state.sessionNumber + 1 else state.sessionNumber,
            updatedAtMillis = nowWallClock,
        )
    }

    /** Manual skip: never auto-starts the next session. */
    fun skip(state: TimerState, config: TimerConfig, nowWallClock: Long): TimerState =
        advance(state, config, nowWallClock, allowAutoStart = false)

    /**
     * Applies edited durations without disturbing a running session. An idle session adopts the
     * new length outright; a paused session keeps its progress but its length is clamped to the
     * new duration, so it can never display more time than the configured session allows.
     */
    fun applyConfig(state: TimerState, config: TimerConfig, nowWallClock: Long): TimerState =
        when (state.status) {
            TimerStatus.IDLE -> initialState(state.sessionType, config, nowWallClock).copy(
                cyclePosition = state.cyclePosition,
                sessionNumber = state.sessionNumber,
            )

            TimerStatus.PAUSED -> {
                val duration = config.durationMillis(state.sessionType)
                state.copy(
                    durationMillis = duration,
                    remainingMillis = state.remainingMillis.coerceIn(0L, duration),
                    updatedAtMillis = nowWallClock,
                )
            }

            TimerStatus.RUNNING -> state
        }

    fun withRemaining(state: TimerState, remainingMillis: Long): TimerState =
        state.copy(remainingMillis = remainingMillis.coerceIn(0L, state.durationMillis))
}
