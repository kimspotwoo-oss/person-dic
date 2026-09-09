package com.persondic.ui.groupmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import kotlin.math.abs

/**
 * The bubble map's job is to be read, and a name printed on top of another name cannot be. These
 * check the placement, not the springs: circles may sit as close as the springs want, but the two
 * names must not land in the same place.
 */
class BubbleLayoutTest {

    private val people = (1..8).map { UUID.randomUUID() }

    private fun bubble(tag: String, vararg indices: Int) =
        GroupBubble(tag = tag, memberIds = indices.map { people[it] }.toSet())

    /** Same estimate BubbleLayout uses, so the assertions test the real threshold. */
    private fun halfWidth(tag: String) = (tag.length + 1) * 12f / 2f / 360f
    private val halfHeight = 17f / 360f

    private fun assertNamesReadable(bubbles: List<GroupBubble>) {
        val positions = layoutBubbles(bubbles)
        for (i in bubbles.indices) {
            for (j in i + 1 until bubbles.size) {
                val a = bubbles[i].tag
                val b = bubbles[j].tag
                val first = positions.getValue(a)
                val second = positions.getValue(b)
                val apart = abs(first.x - second.x) >= halfWidth(a) + halfWidth(b) ||
                    abs(first.y - second.y) >= 2 * halfHeight
                assertTrue("#$a and #$b are printed on top of each other at $first / $second", apart)
            }
        }
    }

    @Test
    fun namesDoNotOverlapWhenTwoGroupsHoldExactlyTheSamePeople() {
        // #대구 and #영남고등학교 covering the same four people printed one name over the other.
        assertNamesReadable(
            listOf(
                bubble("대구", 0, 1, 2, 3),
                bubble("영남고등학교", 0, 1, 2, 3),
                bubble("1학년", 0, 1, 2, 3, 4, 5),
            ),
        )
    }

    @Test
    fun namesDoNotOverlapAcrossAFullBoardOfGroups() {
        assertNamesReadable(
            listOf(
                bubble("06", 0, 1, 2),
                bubble("07", 3, 4, 5),
                bubble("08", 6),
                bubble("1학년", 0, 1, 2, 3, 4, 5),
                bubble("대구", 0, 1, 2, 3),
                bubble("영남고등학교", 0, 1, 2, 3),
                bubble("경북대학교", 4, 5),
                bubble("고려대학교", 0, 1, 2, 6),
                bubble("동국대학교", 7),
                bubble("수학과", 4),
                bubble("전기전자공학부", 5),
                bubble("젊은예수", 0, 1),
            ),
        )
    }

    @Test
    fun namesDoNotOverlapWhenEveryGroupIsIdentical() {
        // Nothing for the springs to separate on: they all want the same spot.
        assertNamesReadable((1..6).map { bubble("그룹$it", 0, 1, 2) })
    }

    @Test
    fun everyCircleStaysInsideTheSquare() {
        val bubbles = listOf(
            bubble("대구", 0, 1, 2, 3),
            bubble("영남고등학교", 0, 1, 2, 3),
            bubble("전기전자공학부", 5),
            bubble("1학년", 0, 1, 2, 3, 4, 5),
        )
        val maxMembers = bubbles.maxOf { it.memberIds.size }
        val positions = layoutBubbles(bubbles)

        bubbles.forEach { bubble ->
            val centre = positions.getValue(bubble.tag)
            val radius = bubbleRadius(bubble.memberIds.size, maxMembers)
            assertTrue("#${bubble.tag} runs off the left at ${centre.x}", centre.x - radius >= -1e-4f)
            assertTrue("#${bubble.tag} runs off the right at ${centre.x}", centre.x + radius <= 1.0001f)
            assertTrue("#${bubble.tag} runs off the top at ${centre.y}", centre.y - radius >= -1e-4f)
            assertTrue("#${bubble.tag} runs off the bottom at ${centre.y}", centre.y + radius <= 1.0001f)
        }
    }

    @Test
    fun theSameGroupsAlwaysLandInTheSamePlace() {
        val bubbles = listOf(bubble("대구", 0, 1), bubble("1학년", 0, 1, 2), bubble("06", 2))

        assertEquals(layoutBubbles(bubbles), layoutBubbles(bubbles))
    }

    @Test
    fun aSingleGroupSitsInTheMiddle() {
        val positions = layoutBubbles(listOf(bubble("대구", 0, 1)))

        assertEquals(0.5f, positions.getValue("대구").x, 1e-6f)
        assertEquals(0.5f, positions.getValue("대구").y, 1e-6f)
    }
}
