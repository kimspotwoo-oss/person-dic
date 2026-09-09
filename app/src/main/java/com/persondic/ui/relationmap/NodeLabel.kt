package com.persondic.ui.relationmap

import com.persondic.ui.common.avatarInitials

/**
 * What to write inside a node of a given size.
 *
 * The layout shrinks the circles rather than letting them collide, which is the right trade until
 * the circle is smaller than the name it has to hold — at which point the ellipsis takes over and
 * every node reads "…", which distinguishes nobody and is worse than the overlap it avoided.
 *
 * So the name gives way in stages. Where the whole name fits it is written out; where it does not,
 * the same two characters the avatars use, which are chosen to tell people apart rather than to
 * read as a name; and below that nothing, because a circle with nothing in it is at least honestly
 * empty and still opens the person when tapped.
 */
fun graphNodeLabel(name: String, diameterDp: Float): String = when {
    diameterDp >= FULL_NAME_DP -> name
    diameterDp >= INITIALS_DP -> avatarInitials(name)
    else -> ""
}

/** Two lines of labelSmall with room to spare — a four-character name still fits. */
private const val FULL_NAME_DP = 40f

/** One line of two characters. */
private const val INITIALS_DP = 24f
