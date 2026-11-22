package com.rabindra.focustime.watchface

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchFaceTest {

    @Test
    fun everyStoredIdIsUniqueAndRoundTrips() {
        val ids = WatchFace.entries.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
        for (face in WatchFace.entries) {
            assertEquals(face, WatchFace.fromId(face.id))
        }
    }

    @Test
    fun unknownOrMissingIdsFallBackToDeskClock() {
        assertEquals(WatchFace.DESK_CLOCK, WatchFace.fromId(null))
        assertEquals(WatchFace.DESK_CLOCK, WatchFace.fromId(""))
        assertEquals(WatchFace.DESK_CLOCK, WatchFace.fromId("not-a-face"))
    }

    @Test
    fun nextAndPreviousCycleThroughEveryFace() {
        for (face in WatchFace.entries) {
            assertEquals(face, face.next().previous())
            assertEquals(face, face.previous().next())
        }

        val first = WatchFace.entries.first()
        val last = WatchFace.entries.last()
        assertEquals(first, last.next())
        assertEquals(last, first.previous())

        // Swiping forwards once per face should visit each face exactly once.
        val visited = generateSequence(first) { it.next() }.take(WatchFace.entries.size).toList()
        assertEquals(WatchFace.entries.toList(), visited)
        assertTrue(visited.toSet().size == WatchFace.entries.size)
    }
}
