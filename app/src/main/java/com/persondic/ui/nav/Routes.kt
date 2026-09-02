package com.persondic.ui.nav

import java.util.UUID

object Routes {
    const val PERSON_LIST = "personList"
    const val FACT_EDIT = "factEdit/{personId}"

    fun factEdit(personId: UUID): String = "factEdit/$personId"
}
