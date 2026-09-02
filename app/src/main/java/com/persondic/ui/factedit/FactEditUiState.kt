package com.persondic.ui.factedit

import com.persondic.data.model.FactCategory
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import java.time.LocalDate

data class FactEditUiState(
    val assertedOn: LocalDate,
    val body: String = "",
    val category: FactCategory = FactCategory.CONTEXT,
    val volatility: Volatility = Volatility.PERMANENT,
    val sensitivity: Sensitivity = Sensitivity.NORMAL,
    val pinned: Boolean = false,
    val expiresOnPreview: LocalDate? = null,
)
