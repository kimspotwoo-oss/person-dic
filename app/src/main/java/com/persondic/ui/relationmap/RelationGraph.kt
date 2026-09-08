package com.persondic.ui.relationmap

import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.Tie
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

data class GraphNode(
    val personId: UUID,
    val name: String,
    val isSelf: Boolean,
    /** Normalized 0..1 within the drawing square. */
    val x: Float,
    val y: Float,
)

data class GraphEdge(
    val tieId: UUID,
    val fromPersonId: UUID,
    val toPersonId: UUID,
    val label: String,
    /** No arrowhead when true; the label reads the same from either end. */
    val symmetric: Boolean,
)

data class RelationGraph(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList(),
)

/**
 * Places everyone on rings around the owner.
 *
 * The owner sits at the centre because the graph is drawn from their point of view. People they
 * are tied to directly form the first ring; people only tied to someone else form the second; the
 * rest go on the outside. Ring order follows each person's name, so the picture is the same every
 * time it is drawn — a layout that reshuffles on every recomposition is unreadable.
 *
 * [visibleLabels] of null means every label; otherwise only ties carrying one of those labels are
 * drawn, and the rings are computed from those ties alone so filtering actually rearranges the
 * picture instead of leaving stragglers floating at their old distance.
 */
fun buildRelationGraph(
    people: List<Person>,
    ties: List<Tie>,
    visibleLabels: Set<String>? = null,
): RelationGraph {
    if (people.isEmpty()) return RelationGraph()

    val known = people.mapTo(mutableSetOf()) { it.id }
    val edges = ties
        .filter { it.fromPersonId in known && it.toPersonId in known }
        .filter { visibleLabels == null || it.label in visibleLabels }
        .map { GraphEdge(it.id, it.fromPersonId, it.toPersonId, it.label, it.symmetric) }

    val neighbours = mutableMapOf<UUID, MutableSet<UUID>>()
    edges.forEach { edge ->
        neighbours.getOrPut(edge.fromPersonId) { mutableSetOf() } += edge.toPersonId
        neighbours.getOrPut(edge.toPersonId) { mutableSetOf() } += edge.fromPersonId
    }

    val self = people.firstOrNull { it.isSelf }
    val others = people.filterNot { it.isSelf }.sortedBy { it.name() }

    // Without an owner row there is no centre to measure from, so everyone shares one ring.
    if (self == null) {
        return RelationGraph(nodes = ring(others, RING_TWO, isSelf = false), edges = edges)
    }

    val direct = others.filter { it.id in neighbours[self.id].orEmpty() }
    val rest = others - direct.toSet()
    val secondHand = rest.filter { person -> neighbours[person.id].orEmpty().any { it != self.id } }
    val unconnected = rest - secondHand.toSet()

    val nodes = buildList {
        add(GraphNode(self.id, self.displayName, isSelf = true, x = CENTRE, y = CENTRE))
        addAll(ring(direct, RING_ONE, isSelf = false))
        addAll(ring(secondHand, RING_TWO, isSelf = false))
        addAll(ring(unconnected, RING_THREE, isSelf = false))
    }
    return RelationGraph(nodes = nodes, edges = edges)
}

private fun Person.name(): String = displayName

/** Evenly spaced around a circle, starting at the top and going clockwise. */
private fun ring(people: List<Person>, radius: Float, isSelf: Boolean): List<GraphNode> {
    if (people.isEmpty()) return emptyList()
    return people.mapIndexed { index, person ->
        val angle = (index.toFloat() / people.size) * TWO_PI - QUARTER_TURN
        GraphNode(
            personId = person.id,
            name = person.displayName,
            isSelf = isSelf,
            x = CENTRE + radius * cos(angle),
            y = CENTRE + radius * sin(angle),
        )
    }
}

private const val CENTRE = 0.5f
private const val RING_ONE = 0.20f
private const val RING_TWO = 0.34f
private const val RING_THREE = 0.46f
private const val TWO_PI = (2.0 * Math.PI).toFloat()
private const val QUARTER_TURN = (Math.PI / 2.0).toFloat()
