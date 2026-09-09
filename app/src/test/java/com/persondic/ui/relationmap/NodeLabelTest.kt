package com.persondic.ui.relationmap

import org.junit.Assert.assertEquals
import org.junit.Test

class NodeLabelTest {

    @Test
    fun aRoomySizedNodeSpellsTheNameOut() {
        assertEquals("김경모", graphNodeLabel("김경모", diameterDp = 52f))
        assertEquals("김경모", graphNodeLabel("김경모", diameterDp = 40f))
    }

    @Test
    fun aTightNodeFallsBackToTheSameTwoCharactersTheAvatarsUse() {
        // 40 people on the outer ring shrinks the circles well past a three-character name.
        assertEquals("경모", graphNodeLabel("김경모", diameterDp = 30f))
        assertEquals("민준", graphNodeLabel("김민준", diameterDp = 24f))
    }

    @Test
    fun aNodeTooSmallForAnythingIsLeftEmptyRatherThanEllipsised() {
        // "…" in every circle tells the reader less than an empty circle does, and the circle is
        // still tappable either way.
        assertEquals("", graphNodeLabel("김경모", diameterDp = 23f))
        assertEquals("", graphNodeLabel("김경모", diameterDp = 8f))
    }

    @Test
    fun theFallbackStillTellsTheThreeKimsApart() {
        val tight = listOf("김경모", "김민준", "김범채").map { graphNodeLabel(it, diameterDp = 28f) }

        assertEquals(tight.size, tight.toSet().size)
    }
}
