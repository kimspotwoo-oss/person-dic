package com.persondic.ui.nav

import java.util.UUID

object Routes {
    const val PERSON_LIST = "personList"
    const val PERSON_DETAIL = "personDetail/{personId}"
    const val FACT_EDIT = "factEdit/{personId}?factId={factId}"
    const val BRIEFING = "briefing/{personId}"
    const val INTERACTION_LOG = "interactionLog/{personId}"

    fun personDetail(personId: UUID): String = "personDetail/$personId"

    fun addFact(personId: UUID): String = "factEdit/$personId"

    fun editFact(personId: UUID, factId: UUID): String = "factEdit/$personId?factId=$factId"

    fun briefing(personId: UUID): String = "briefing/$personId"

    fun interactionLog(personId: UUID): String = "interactionLog/$personId"
}
