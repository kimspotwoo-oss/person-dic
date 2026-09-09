package com.persondic.ui.groupmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import kotlin.math.hypot

class VennLabelTest {

    private fun bubble(tag: String, size: Int) =
        GroupBubble(tag, (1..size).map { UUID.randomUUID() }.toSet())

    @Test
    fun eachCircleGetsItsNameOnItsOwnSide() {
        val circles = vennCircles(listOf(bubble("고려대학교", 4), bubble("1학년", 6), bubble("06", 3)))
        val anchors = vennLabelAnchors(circles)

        assertEquals(3, anchors.size)
        circles.forEach { circle ->
            val anchor = anchors.getValue(circle.tag)
            val fromOwnCentre = hypot(anchor.x - circle.center.x, anchor.y - circle.center.y)
            assertTrue("#${circle.tag} sits outside its own circle", fromOwnCentre < circle.radius)

            // Further from every other circle's centre than from its own: that is what makes it
            // land in the lobe belonging to this group alone.
            circles.filter { it.tag != circle.tag }.forEach { other ->
                val fromOther = hypot(anchor.x - other.center.x, anchor.y - other.center.y)
                assertTrue("#${circle.tag}'s name drifted towards #${other.tag}", fromOther > fromOwnCentre)
            }
        }
    }

    @Test
    fun twoCirclesPutTheirNamesOnOppositeSides() {
        val circles = vennCircles(listOf(bubble("대구", 4), bubble("영남고등학교", 4)))
        val anchors = vennLabelAnchors(circles)

        val left = anchors.getValue("대구")
        val right = anchors.getValue("영남고등학교")
        assertTrue("the two names landed on the same side", left.x < right.x)
    }

    @Test
    fun nothingSelectedMeansNothingToLabel() {
        assertEquals(emptyMap<String, Any>(), vennLabelAnchors(emptyList()))
    }
}
