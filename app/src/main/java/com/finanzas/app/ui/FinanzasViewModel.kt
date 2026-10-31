package com.finanzas.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.app.data.FinanzasRepository
import com.finanzas.app.data.Ledger
import com.finanzas.app.data.MovementType
import com.finanzas.app.widget.WidgetStyle
import com.finanzas.app.widget.WidgetStyleStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class FinanzasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanzasRepository.get(application)

    val ledger: StateFlow<Ledger> = repository.ledger
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ledger.EMPTY)

    private val _widgetStyle = MutableStateFlow(WidgetStyleStore.load(application))
    val widgetStyle: StateFlow<WidgetStyle> = _widgetStyle.asStateFlow()

    init {
        // Guarda el estilo y refresca el widget cuando se deja de mover un control.
        viewModelScope.launch {
            _widgetStyle.drop(1).debounce(400).collect { style ->
                WidgetStyleStore.save(getApplication(), style)
                repository.refreshWidget()
            }
        }
    }

    fun updateWidgetStyle(style: WidgetStyle) {
        _widgetStyle.value = style
    }

    fun add(type: MovementType, amountCents: Long, description: String, expectedEpochDay: Long?) =
        viewModelScope.launch { repository.add(type, amountCents, description, expectedEpochDay) }

    fun edit(id: Long, amountCents: Long, description: String, expectedEpochDay: Long?) =
        viewModelScope.launch { repository.edit(id, amountCents, description, expectedEpochDay) }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }

    fun payWithIncome(id: Long) = viewModelScope.launch { repository.payWithIncome(id) }

    fun markReceived(id: Long) = viewModelScope.launch { repository.markReceived(id) }

    fun undoPaid(id: Long) = viewModelScope.launch { repository.undoPaid(id) }
}
