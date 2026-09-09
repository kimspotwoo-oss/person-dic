package com.persondic.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

/**
 * Built from the tags actually in use, because that is where the problem showed: seven people whose
 * tag sets overlap so heavily that printing all of them says nothing.
 */
class DistinguishingTagsTest {

    private val gyeongmo = UUID.randomUUID()
    private val minjun = UUID.randomUUID()
    private val chanyoung = UUID.randomUUID()
    private val wonho = UUID.randomUUID()
    private val jinhoon = UUID.randomUUID()
    private val hayoung = UUID.randomUUID()
    private val beomchae = UUID.randomUUID()

    private val everyone = mapOf(
        gyeongmo to listOf("07", "1학년", "경북대학교", "대구", "영남고등학교"),
        minjun to listOf("07", "1학년", "고려대학교", "대구", "영남고등학교"),
        chanyoung to listOf("07", "1학년", "경북대학교", "대구", "영남고등학교"),
        wonho to listOf("08", "1학년", "대구", "동국대학교", "영남고등학교"),
        jinhoon to listOf("06", "고려대학교", "전기전자공학부"),
        hayoung to listOf("06", "1학년", "고려대학교", "수학과", "젊은예수"),
        beomchae to listOf("06", "1학년", "고려대학교", "젊은예수"),
    )

    @Test
    fun theRarestTagComesFirst() {
        val picked = distinguishingTags(everyone)

        // #08 and #동국대학교 are 이원호's alone; #1학년 is on six of seven and never worth a row.
        assertEquals(listOf("08", "동국대학교"), picked[wonho])
        assertEquals(listOf("전기전자공학부", "06"), picked[jinhoon])
        assertEquals(listOf("수학과", "젊은예수"), picked[hayoung])
    }

    @Test
    fun theTagEveryoneSharesIsDropped() {
        distinguishingTags(everyone).values.forEach { tags ->
            assertEquals("#1학년 is on six of seven people and tells them apart from nobody",
                false, "1학년" in tags)
        }
    }

    @Test
    fun peopleWithDifferentSchoolsNoLongerReadTheSame() {
        val picked = distinguishingTags(everyone)

        // 김경모 and 김민준 differ only by university, and that is now the first thing on the row.
        assertEquals(listOf("경북대학교", "07"), picked[gyeongmo])
        assertEquals(listOf("07", "고려대학교"), picked[minjun])
    }

    @Test
    fun identicalTagSetsStillReadIdentically() {
        // 김경모 and 박찬영 carry exactly the same tags, so no choice of tags can separate them.
        // The row falls back to when they were last met; this is a limit, not a bug.
        val picked = distinguishingTags(everyone)

        assertEquals(picked[gyeongmo], picked[chanyoung])
    }

    @Test
    fun aPersonWithFewerTagsThanTheLimitKeepsThemAll() {
        val single = UUID.randomUUID()
        val picked = distinguishingTags(mapOf(single to listOf("대구")), limit = 2)

        assertEquals(listOf("대구"), picked[single])
    }

    @Test
    fun theSamePeopleAlwaysProduceTheSameRows() {
        assertEquals(distinguishingTags(everyone), distinguishingTags(everyone))
    }

    @Test
    fun aLimitOfZeroPrintsNothing() {
        distinguishingTags(everyone, limit = 0).values.forEach { assertEquals(emptyList<String>(), it) }
    }

    @Test
    fun duplicateTagsOnOnePersonAreCountedOnce() {
        val a = UUID.randomUUID()
        val b = UUID.randomUUID()
        // "대구" twice on one person must not make it look twice as common as it is.
        val picked = distinguishingTags(
            mapOf(a to listOf("대구", "대구", "수학과"), b to listOf("수학과")),
            limit = 1,
        )

        assertEquals(listOf("대구"), picked[a])
    }

    @Test
    fun nobodyOnTheListMeansNothingToPick() {
        assertEquals(emptyMap<UUID, List<String>>(), distinguishingTags(emptyMap()))
    }
}
