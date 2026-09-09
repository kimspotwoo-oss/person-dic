package com.persondic.ui.relationmap

import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.Tie
import com.persondic.domain.dedupeTies
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
    /**
     * How big to draw each node, in the same normalized units as [GraphNode.x].
     *
     * The layout decides this rather than the screen, because the ring radii and the node size are
     * one calculation: rings have to clear each other by a node diameter, and the outermost ring
     * has to leave a node radius before the edge of the square. Drawing at any other size undoes
     * both guarantees.
     */
    val nodeRadius: Float = MAX_NODE_RADIUS,
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
    // The same friendship entered from both people's screens is two rows and was two lines drawn
    // exactly on top of each other.
    val edges = dedupeTies(ties)
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
    val rings = if (self == null) {
        listOf(others)
    } else {
        val direct = others.filter { it.id in neighbours[self.id].orEmpty() }
        val rest = others - direct.toSet()
        val secondHand = rest.filter { person -> neighbours[person.id].orEmpty().any { it != self.id } }
        listOf(direct, secondHand, rest - secondHand.toSet())
    }

    val plan = planRings(rings.map { it.size }, hasSelf = self != null)
    val nodes = buildList {
        if (self != null) {
            add(GraphNode(self.id, self.displayName, isSelf = true, x = CENTRE, y = CENTRE))
        }
        rings.forEachIndexed { index, members ->
            // Every other ring starts half a step round, so a person on one ring does not sit
            // directly outside a person on the next and read as one blob with them.
            addAll(ring(members, plan.radii[index], stagger = index % 2 == 1))
        }
    }
    return RelationGraph(nodes = nodes, edges = edges, nodeRadius = plan.nodeRadius)
}

private fun Person.name(): String = displayName

/** Evenly spaced around a circle, starting at the top and going clockwise. */
private fun ring(people: List<Person>, radius: Float, stagger: Boolean): List<GraphNode> {
    if (people.isEmpty()) return emptyList()
    val step = TWO_PI / people.size
    val start = -QUARTER_TURN + if (stagger) step / 2f else 0f
    return people.mapIndexed { index, person ->
        val angle = start + index * step
        GraphNode(
            personId = person.id,
            name = person.displayName,
            isSelf = false,
            x = CENTRE + radius * cos(angle),
            y = CENTRE + radius * sin(angle),
        )
    }
}

private class RingPlan(val nodeRadius: Float, val radii: List<Float>)

/**
 * Works out the ring radii and the node size that lets them all fit.
 *
 * Both lower bounds on a ring are multiples of the node radius — a ring has to clear the one
 * inside it by a node diameter, and its own neighbours have to be a node diameter apart along the
 * ring — so the whole plan is first computed in units of the node radius and only then scaled.
 * Once the outermost ring is `k` units out, the node radius `r` has to satisfy `k·r + r <= 0.5`
 * for the outermost circle to stay inside the square, which gives the size below.
 *
 * Doing it the other way round is what broke the picture before: fixed radii of 0.20 / 0.34 / 0.46
 * with a fixed 26dp node meant the outer ring sat 16dp from the edge of a 411dp screen and its
 * circles hung 10dp off it, while the 0.12 gap to the middle ring came to 49dp — less than the
 * 52dp the two circles needed.
 */
private fun planRings(counts: List<Int>, hasSelf: Boolean): RingPlan {
    // The owner is a node too, so the first ring has to clear the centre.
    var previous: Float? = if (hasSelf) 0f else null
    var outermost = 0f
    val units = counts.map { count ->
        if (count == 0) return@map 0f
        val clearsInnerRing = previous?.plus(2f * MIN_GAP) ?: 0f
        // Chord between neighbours on a ring of radius R is 2·R·sin(pi/n).
        val clearsNeighbours = if (count >= 2) MIN_GAP / sin(Math.PI / count).toFloat() else 0f
        val radius = maxOf(clearsInnerRing, clearsNeighbours)
        previous = radius
        outermost = radius
        radius
    }
    val nodeRadius = minOf(MAX_NODE_RADIUS, HALF_SQUARE / (outermost + 1f))
    return RingPlan(nodeRadius, units.map { it * nodeRadius })
}

private const val CENTRE = 0.5f
private const val HALF_SQUARE = 0.5f

/** Circles are this much further apart than touching, so they read as separate. */
private const val MIN_GAP = 1.15f

/** Preferred node size — about 26dp on a 411dp-wide phone. Shrinks when the rings need the room. */
const val MAX_NODE_RADIUS = 0.063f

private const val TWO_PI = (2.0 * Math.PI).toFloat()
private const val QUARTER_TURN = (Math.PI / 2.0).toFloat()
