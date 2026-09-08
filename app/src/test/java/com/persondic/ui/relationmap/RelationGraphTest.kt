package com.persondic.ui.relationmap

import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.Tie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import kotlin.math.hypot

class RelationGraphTest {

    private val me = Person(id = UUID.randomUUID(), displayName = "나", isSelf = true)
    private val minjun = Person(id = UUID.randomUUID(), displayName = "김민준")
    private val seoyeon = Person(id = UUID.randomUUID(), displayName = "이서연")
    private val jiho = Person(id = UUID.randomUUID(), displayName = "박지호")

    private fun tie(from: Person, to: Person, label: String, symmetric: Boolean = false) =
        Tie(fromPersonId = from.id, toPersonId = to.id, label = label, symmetric = symmetric)

    private fun distanceFromCentre(node: GraphNode) = hypot(node.x - 0.5f, node.y - 0.5f)

    private fun RelationGraph.node(person: Person) = nodes.first { it.personId == person.id }

    @Test
    fun theOwnerSitsAtTheCentre() {
        val graph = buildRelationGraph(listOf(me, minjun), listOf(tie(me, minjun, "친구", true)))

        val self = graph.nodes.single { it.isSelf }
        assertEquals(0.5f, self.x, 1e-5f)
        assertEquals(0.5f, self.y, 1e-5f)
    }

    @Test
    fun someoneTiedToMeSitsCloserThanSomeoneTiedOnlyToThem() {
        val graph = buildRelationGraph(
            people = listOf(me, minjun, seoyeon),
            ties = listOf(tie(me, minjun, "친구", true), tie(minjun, seoyeon, "배우자", true)),
        )

        assertTrue(distanceFromCentre(graph.node(minjun)) < distanceFromCentre(graph.node(seoyeon)))
    }

    @Test
    fun someoneWithNoTiesSitsFurthestOut() {
        val graph = buildRelationGraph(
            people = listOf(me, minjun, seoyeon, jiho),
            ties = listOf(tie(me, minjun, "친구", true), tie(minjun, seoyeon, "배우자", true)),
        )

        val outer = distanceFromCentre(graph.node(jiho))
        assertTrue(outer > distanceFromCentre(graph.node(seoyeon)))
        assertTrue(outer > distanceFromCentre(graph.node(minjun)))
    }

    @Test
    fun everyoneGetsANodeExactlyOnce() {
        val people = listOf(me, minjun, seoyeon, jiho)
        val graph = buildRelationGraph(people, listOf(tie(me, minjun, "친구", true)))

        assertEquals(people.size, graph.nodes.size)
        assertEquals(people.map { it.id }.toSet(), graph.nodes.map { it.personId }.toSet())
    }

    @Test
    fun theLayoutIsTheSameEveryTimeAndIgnoresInputOrder() {
        val ties = listOf(tie(me, minjun, "친구", true), tie(me, seoyeon, "소개해준 사람"))

        val first = buildRelationGraph(listOf(me, minjun, seoyeon, jiho), ties)
        val second = buildRelationGraph(listOf(jiho, seoyeon, minjun, me), ties.reversed())

        assertEquals(
            first.nodes.sortedBy { it.name }.map { Triple(it.name, it.x, it.y) },
            second.nodes.sortedBy { it.name }.map { Triple(it.name, it.x, it.y) },
        )
    }

    @Test
    fun everyNodeStaysInsideTheDrawingSquare() {
        val graph = buildRelationGraph(
            people = listOf(me, minjun, seoyeon, jiho),
            ties = listOf(tie(me, minjun, "친구", true)),
        )

        assertTrue(graph.nodes.all { it.x in 0f..1f && it.y in 0f..1f })
    }

    @Test
    fun aSymmetricTieIsMarkedAsSuchAndADirectedOneIsNot() {
        val graph = buildRelationGraph(
            people = listOf(me, minjun, seoyeon),
            ties = listOf(tie(me, minjun, "친구", true), tie(me, seoyeon, "소개해준 사람")),
        )

        assertTrue(graph.edges.first { it.label == "친구" }.symmetric)
        assertTrue(!graph.edges.first { it.label == "소개해준 사람" }.symmetric)
    }

    @Test
    fun filteringByLabelKeepsOnlyThoseEdges() {
        val graph = buildRelationGraph(
            people = listOf(me, minjun, seoyeon),
            ties = listOf(tie(me, minjun, "친구", true), tie(me, seoyeon, "소개해준 사람")),
            visibleLabels = setOf("친구"),
        )

        assertEquals(listOf("친구"), graph.edges.map { it.label })
    }

    @Test
    fun filteringMovesPeopleWhoseOnlyTieWasHidden() {
        val ties = listOf(tie(me, minjun, "친구", true), tie(me, seoyeon, "소개해준 사람"))

        val unfiltered = buildRelationGraph(listOf(me, minjun, seoyeon), ties)
        val friendsOnly = buildRelationGraph(listOf(me, minjun, seoyeon), ties, setOf("친구"))

        // 이서연 was on the inner ring while her tie showed; hiding it must push her out.
        assertTrue(
            distanceFromCentre(friendsOnly.node(seoyeon)) >
                distanceFromCentre(unfiltered.node(seoyeon)),
        )
    }

    @Test
    fun aTiePointingAtSomeoneWhoIsGoneIsDropped() {
        val graph = buildRelationGraph(listOf(me, minjun), listOf(tie(me, jiho, "친구", true)))

        assertTrue(graph.edges.isEmpty())
    }

    @Test
    fun withNoOwnerRowEveryoneStillGetsPlaced() {
        val graph = buildRelationGraph(listOf(minjun, seoyeon), listOf(tie(minjun, seoyeon, "배우자", true)))

        assertEquals(2, graph.nodes.size)
        assertTrue(graph.nodes.none { it.isSelf })
        assertTrue(graph.nodes.all { it.x in 0f..1f && it.y in 0f..1f })
    }

    @Test
    fun anEmptyDatabaseDrawsNothing() {
        val graph = buildRelationGraph(emptyList(), emptyList())

        assertTrue(graph.nodes.isEmpty())
        assertTrue(graph.edges.isEmpty())
    }

    @Test
    fun peopleOnTheSameRingDoNotLandOnTopOfEachOther() {
        val graph = buildRelationGraph(
            people = listOf(me, minjun, seoyeon, jiho),
            ties = listOf(
                tie(me, minjun, "친구", true),
                tie(me, seoyeon, "친구", true),
                tie(me, jiho, "친구", true),
            ),
        )

        val ring = graph.nodes.filterNot { it.isSelf }
        ring.forEachIndexed { i, a ->
            ring.drop(i + 1).forEach { b ->
                assertTrue("$a and $b overlap", hypot(a.x - b.x, a.y - b.y) > 0.05f)
            }
        }
        assertNotNull(graph.nodes.singleOrNull { it.isSelf })
    }
}
