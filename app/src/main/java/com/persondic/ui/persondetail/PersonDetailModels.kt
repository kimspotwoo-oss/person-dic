package com.persondic.ui.persondetail

import com.persondic.data.local.entity.Fact
import com.persondic.data.model.FactCategory

data class FactCategoryGroup(
    val category: FactCategory,
    val facts: List<Fact>,
)
