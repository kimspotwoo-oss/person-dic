package com.persondic.ui.briefing

import com.persondic.data.local.entity.Fact
import com.persondic.data.local.entity.Person
import com.persondic.data.local.entity.PersonAttribute
import com.persondic.data.model.FactCategory
import com.persondic.data.model.Volatility
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class BriefingUiStateTest {

    private val personId = UUID.randomUUID()

    private fun fact(category: FactCategory = FactCategory.HOOK) = Fact(
        personId = personId,
        category = category,
        body = "테니스를 시작했다",
        volatility = Volatility.SEASONAL,
        assertedOn = LocalDate.of(2026, 1, 1),
    )

    @Test
    fun aPersonWithNothingRecordedHasNothingToBriefOn() {
        assertTrue(BriefingUiState(personName = "김민준").hasNothingToShow)
    }

    @Test
    fun oneFactAnywhereIsEnoughToHaveSomethingToSay() {
        assertFalse(BriefingUiState(hookFacts = listOf(fact())).hasNothingToShow)
        assertFalse(BriefingUiState(cautionFacts = listOf(fact())).hasNothingToShow)
        assertFalse(BriefingUiState(sensitiveFacts = listOf(fact())).hasNothingToShow)
        assertFalse(BriefingUiState(staleFacts = listOf(fact())).hasNothingToShow)
    }

    @Test
    fun aBirthdayAloneIsStillNothingToBriefOn() {
        // Fixed information is who someone is, not news about them, and it draws its own block
        // above the sections. A page showing only that still needs to say why it is otherwise bare.
        val state = BriefingUiState(
            person = Person(id = personId, displayName = "김민준", birthYear = 2006),
            attributes = listOf(PersonAttribute(personId = personId, label = "학번", value = "26")),
        )

        assertTrue(state.hasNothingToShow)
    }
}
