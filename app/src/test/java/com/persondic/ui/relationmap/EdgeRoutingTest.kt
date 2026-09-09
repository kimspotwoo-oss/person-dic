package com.persondic.ui.relationmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot

class EdgeRoutingTest {

    private val radius = 0.06f
    private val clearance = radius * 1.8f

    private fun node(name: String, x: Float, y: Float) =
        GraphNode(UUID.randomUUID(), name, isSelf = false, x = x, y = y)

    private fun edge(from: GraphNode, to: GraphNode, label: String = "친구") =
        GraphEdge(UUID.randomUUID(), from.personId, to.personId, label, symmetric = true)

    /** How far the drawn curve passes from a point, at the place it bends furthest. */
    private fun curveClearsBy(from: GraphNode, to: GraphNode, obstacle: GraphNode, bow: Float): Float {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val length = hypot(dx, dy)
        val perpX = -dy / length
        val perpY = dx / length
        val across = (obstacle.x - from.x) * perpX + (obstacle.y - from.y) * perpY
        // A quadratic reaches half its control offset at its furthest point.
        return abs(bow / 2f - across)
    }

    @Test
    fun aClearLineIsLeftStraight() {
        val a = node("가", 0.2f, 0.5f)
        val b = node("나", 0.8f, 0.5f)
        val bystander = node("멀리", 0.5f, 0.9f)

        val routed = routeEdges(listOf(edge(a, b)), listOf(a, b, bystander), radius)

        assertEquals(0f, routed.single().bow, 1e-6f)
    }

    @Test
    fun aLineBendsAroundSomebodyStandingInIt() {
        // 김민준 stood between 양하영 and 김범채 and swallowed the friendship between them.
        val hayoung = node("양하영", 0.2f, 0.5f)
        val beomchae = node("김범채", 0.8f, 0.5f)
        val minjun = node("김민준", 0.5f, 0.5f)

        val bow = routeEdges(listOf(edge(hayoung, beomchae)), listOf(hayoung, beomchae, minjun), radius)
            .single().bow

        assertNotEquals(0f, bow)
        assertTrue(
            "the curve passes ${curveClearsBy(hayoung, beomchae, minjun, bow)} from 김민준, " +
                "and needs $clearance",
            curveClearsBy(hayoung, beomchae, minjun, bow) >= clearance - 1e-4f,
        )
    }

    @Test
    fun aNearMissStillGetsPushedClear() {
        val a = node("가", 0.1f, 0.5f)
        val b = node("나", 0.9f, 0.5f)
        // Not on the line, but close enough that the line would graze the circle.
        val grazed = node("스침", 0.5f, 0.55f)

        val bow = routeEdges(listOf(edge(a, b)), listOf(a, b, grazed), radius).single().bow

        assertTrue(curveClearsBy(a, b, grazed, bow) >= clearance - 1e-4f)
    }

    @Test
    fun somebodyPastTheEndIsNotInTheWay() {
        val a = node("가", 0.4f, 0.5f)
        val b = node("나", 0.6f, 0.5f)
        // Dead on the line's direction, but outside the stretch between the two ends.
        val beyond = node("바깥", 0.9f, 0.5f)

        val routed = routeEdges(listOf(edge(a, b)), listOf(a, b, beyond), radius)

        assertEquals(0f, routed.single().bow, 1e-6f)
    }

    @Test
    fun twoRelationsBetweenTheSamePairAreDrawnApart() {
        val a = node("가", 0.2f, 0.5f)
        val b = node("나", 0.8f, 0.5f)
        val bows = routeEdges(
            listOf(edge(a, b, "친구"), edge(a, b, "직장 동료")),
            listOf(a, b),
            radius,
        ).map { it.bow }

        assertEquals(2, bows.size)
        assertNotEquals(bows[0], bows[1])
        assertTrue("the two lines are ${abs(bows[0] - bows[1])} apart", abs(bows[0] - bows[1]) >= radius)
    }

    @Test
    fun theFirstOfSeveralKeepsTheDirectPath() {
        val a = node("가", 0.2f, 0.5f)
        val b = node("나", 0.8f, 0.5f)

        val bows = routeEdges(
            listOf(edge(a, b, "친구"), edge(a, b, "선배"), edge(a, b, "직장 동료")),
            listOf(a, b),
            radius,
        ).map { it.bow }

        assertEquals(0f, bows[0], 1e-6f)
        assertEquals(3, bows.distinct().size)
    }

    @Test
    fun aPairEnteredEitherWayRoundCountsAsTheSamePair() {
        val a = node("가", 0.2f, 0.5f)
        val b = node("나", 0.8f, 0.5f)

        val bows = routeEdges(
            listOf(edge(a, b, "친구"), edge(b, a, "소개해준 사람")),
            listOf(a, b),
            radius,
        ).map { it.bow }

        assertNotEquals(bows[0], bows[1])
    }

    @Test
    fun theSameGraphAlwaysBendsTheSameWay() {
        val a = node("가", 0.2f, 0.5f)
        val b = node("나", 0.8f, 0.5f)
        val between = node("사이", 0.5f, 0.5f)
        val edges = listOf(edge(a, b))

        assertEquals(
            routeEdges(edges, listOf(a, b, between), radius).map { it.bow },
            routeEdges(edges, listOf(a, b, between), radius).map { it.bow },
        )
    }

    @Test
    fun anEdgeToSomebodyNotOnTheGraphIsLeftAlone() {
        val a = node("가", 0.2f, 0.5f)
        val missing = node("없음", 0.8f, 0.5f)

        val routed = routeEdges(listOf(edge(a, missing)), listOf(a), radius)

        assertEquals(0f, routed.single().bow, 1e-6f)
    }
}
