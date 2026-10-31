package com.finanzas.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.app.data.FinanzasRepository
import com.finanzas.app.data.Ledger
import com.finanzas.app.data.MovementType
import com.finanzas.app.data.PaidWith
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanzasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FinanzasRepository.get(application)

    val ledger: StateFlow<Ledger> = repository.ledger
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ledger.EMPTY)

    fun add(type: MovementType, amountCents: Long, description: String) = viewModelScope.launch {
        repository.add(type, amountCents, description)
    }

    fun edit(id: Long, amountCents: Long, description: String) = viewModelScope.launch {
        repository.edit(id, amountCents, description)
    }

    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }

    fun markPaid(id: Long, paidWith: PaidWith) = viewModelScope.launch {
        repository.markPaid(id, paidWith)
    }

    fun undoPaid(id: Long) = viewModelScope.launch { repository.undoPaid(id) }
}
