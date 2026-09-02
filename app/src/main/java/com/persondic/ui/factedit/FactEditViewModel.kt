package com.persondic.ui.factedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.persondic.data.local.entity.Fact
import com.persondic.data.model.FactCategory
import com.persondic.data.model.Sensitivity
import com.persondic.data.model.Volatility
import com.persondic.data.repository.PersonDicRepository
import com.persondic.domain.ExpirationCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class FactEditViewModel(
    private val repository: PersonDicRepository,
    private val personId: UUID,
) : ViewModel() {

    private val assertedOn: LocalDate = LocalDate.now()

    private val _uiState = MutableStateFlow(FactEditUiState(assertedOn = assertedOn))
    val uiState: StateFlow<FactEditUiState> = _uiState.asStateFlow()

    fun onBodyChange(body: String) {
        _uiState.update { it.copy(body = body) }
    }

    fun onCategoryChange(category: FactCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onVolatilityChange(volatility: Volatility) {
        _uiState.update {
            it.copy(
                volatility = volatility,
                expiresOnPreview = ExpirationCalculator.calculateExpiresOn(volatility, assertedOn),
            )
        }
    }

    fun onSensitivityChange(sensitivity: Sensitivity) {
        _uiState.update { it.copy(sensitivity = sensitivity) }
    }

    fun onPinnedChange(pinned: Boolean) {
        _uiState.update { it.copy(pinned = pinned) }
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        val body = state.body.trim()
        if (body.isEmpty()) return
        viewModelScope.launch {
            repository.addFact(
                Fact(
                    personId = personId,
                    category = state.category,
                    body = body,
                    volatility = state.volatility,
                    assertedOn = assertedOn,
                    sensitivity = state.sensitivity,
                    pinned = state.pinned,
                ),
            )
            onSaved()
        }
    }
}
