package com.persondic.domain

import com.persondic.data.model.Direction
import com.persondic.data.model.FactCategory
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddParserTest {

    @Test
    fun parsesNameAndTagsFromTheFirstLine() {
        val result = parseQuickAdd("김민준 @대학동창 @등산")

        val person = result.people.single()
        assertEquals("김민준", person.displayName)
        assertEquals(listOf("대학동창", "등산"), person.tags)
    }

    @Test
    fun keepsSpacesInsideNames() {
        val result = parseQuickAdd("김 민준 @회사")

        assertEquals("김 민준", result.people.single().displayName)
        assertEquals(listOf("회사"), result.people.single().tags)
    }

    @Test
    fun acceptsHashPrefixForTagsToo() {
        val result = parseQuickAdd("이서연 #회사")

        assertEquals(listOf("회사"), result.people.single().tags)
    }

    @Test
    fun parsesAliasAndMetStory() {
        val result = parseQuickAdd(
            """
            김민준 @대학동창
            별명: 민준이
            계기: 동아리에서 처음 만남
            """.trimIndent(),
        )

        val person = result.people.single()
        assertEquals("민준이", person.alias)
        assertEquals("동아리에서 처음 만남", person.metStory)
    }

    @Test
    fun parsesFactsWithDefaults() {
        val result = parseQuickAdd(
            """
            김민준
            - 아들 고3
            """.trimIndent(),
        )

        val fact = result.people.single().facts.single()
        assertEquals("아들 고3", fact.body)
        assertEquals(FactCategory.LIFE, fact.category)
        assertEquals(Volatility.SEASONAL, fact.volatility)
        assertEquals(Sensitivity.NORMAL, fact.sensitivity)
        assertEquals(false, fact.pinned)
    }

    @Test
    fun markersOverrideFactDefaults() {
        val result = parseQuickAdd(
            """
            김민준
            - 갑각류 알레르기 *영구 *주의
            - 커피보다 차 *취향 *천천히
            - 다음에 등산 얘기 *화제
            - 건강 문제 *비공개
            """.trimIndent(),
        )

        val facts = result.people.single().facts
        assertEquals(4, facts.size)

        assertEquals("갑각류 알레르기", facts[0].body)
        assertEquals(Volatility.PERMANENT, facts[0].volatility)
        assertTrue(facts[0].pinned)

        assertEquals("커피보다 차", facts[1].body)
        assertEquals(FactCategory.PREFERENCE, facts[1].category)
        assertEquals(Volatility.SLOW, facts[1].volatility)

        assertEquals(FactCategory.HOOK, facts[2].category)

        assertEquals(Sensitivity.PRIVATE, facts[3].sensitivity)
        assertEquals("건강 문제", facts[3].body)
    }

    @Test
    fun parsesCommitmentDirections() {
        val result = parseQuickAdd(
            """
            김민준
            ? 이직 결과 물어보기
            ! 책 빌려주기
            """.trimIndent(),
        )

        val commitments = result.people.single().commitments
        assertEquals(2, commitments.size)
        assertEquals(ParsedCommitment("이직 결과 물어보기", Direction.THEY_OWE), commitments[0])
        assertEquals(ParsedCommitment("책 빌려주기", Direction.I_OWE), commitments[1])
    }

    @Test
    fun splitsMultiplePeopleOnSeparator() {
        val result = parseQuickAdd(
            """
            김민준 @대학동창
            - 아들 고3

            ---

            이서연 @회사
            - 커피보다 차 *취향
            """.trimIndent(),
        )

        assertEquals(2, result.people.size)
        assertEquals("김민준", result.people[0].displayName)
        assertEquals("이서연", result.people[1].displayName)
        assertEquals(1, result.people[1].facts.size)
    }

    @Test
    fun ignoresBlankInputEntirely() {
        val result = parseQuickAdd("\n\n   \n")

        assertTrue(result.people.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun reportsUnknownLinesInsteadOfSwallowingThem() {
        val result = parseQuickAdd(
            """
            김민준
            이건 무슨 줄이지
            """.trimIndent(),
        )

        assertEquals(1, result.people.size)
        assertEquals(1, result.warnings.size)
        assertTrue(result.warnings.single().contains("이건 무슨 줄이지"))
    }

    @Test
    fun blockWithoutNameIsReportedNotSilentlyDropped() {
        val result = parseQuickAdd("- 이름 없는 사실")

        assertTrue(result.people.isEmpty())
        assertEquals(1, result.warnings.size)
    }

    @Test
    fun deduplicatesRepeatedTags() {
        val result = parseQuickAdd("김민준 @회사 @회사")

        assertEquals(listOf("회사"), result.people.single().tags)
    }

    @Test
    fun aliasAndStoryAreNullWhenAbsent() {
        val person = parseQuickAdd("김민준").people.single()

        assertNull(person.alias)
        assertNull(person.metStory)
    }

    @Test
    fun factBodySurvivesWhenOnlyMarkersAreGiven() {
        val fact = parseQuickAdd(
            """
            김민준
            - *영구
            """.trimIndent(),
        ).people.single().facts.single()

        assertTrue(fact.body.isNotEmpty())
        assertEquals(Volatility.PERMANENT, fact.volatility)
    }
}
