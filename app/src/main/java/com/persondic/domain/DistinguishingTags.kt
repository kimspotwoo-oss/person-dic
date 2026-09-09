package com.persondic.domain

import java.util.UUID

/**
 * Picks the few tags that tell each person apart from everyone else on the list.
 *
 * A list row has one line to say who this is, and printing every tag wastes it. In a real set of
 * people the tags overlap almost completely — six of seven share #1학년, four share #대구 and
 * #영남고등학교 — so a row reading "#07 #1학년 #경북대학교 #대구 #영남고등학교" spends two lines
 * repeating what is true of nearly everyone. Two people with the same tags got byte-identical
 * subtitles and could not be told apart at all.
 *
 * So the tags are ranked by how few people carry them and the rarest come first: #경북대학교 (2 of
 * 7) says more than #1학년 (6 of 7). Ties break on the tag itself, so the same set of people always
 * produces the same rows.
 *
 * Counted over everyone, not over whatever is on screen — otherwise typing in the search box would
 * quietly rewrite the rows that survive it.
 */
fun distinguishingTags(
    tagsByPerson: Map<UUID, List<String>>,
    limit: Int = 2,
): Map<UUID, List<String>> {
    if (limit <= 0) return tagsByPerson.mapValues { emptyList() }

    val carriers = mutableMapOf<String, Int>()
    tagsByPerson.values.forEach { tags ->
        tags.distinct().forEach { tag -> carriers[tag] = (carriers[tag] ?: 0) + 1 }
    }

    return tagsByPerson.mapValues { (_, tags) ->
        tags.distinct()
            .sortedWith(compareBy({ carriers[it] ?: 0 }, { it }))
            .take(limit)
    }
}
