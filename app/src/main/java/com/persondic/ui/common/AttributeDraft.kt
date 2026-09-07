package com.persondic.ui.common

import com.persondic.data.model.Sensitivity

/** One fixed-information entry being edited, before it has a person to belong to. */
data class AttributeDraft(
    val label: String,
    val value: String,
    val sensitivity: Sensitivity = Sensitivity.NORMAL,
)
