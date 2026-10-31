package com.finanzas.app.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.finanzas.app.widget.FinanzasWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanzasRepository private constructor(private val context: Context) {
    private val dao = FinanzasDatabase.get(context).movementDao()

    val ledger: Flow<Ledger> = dao.observeAll().map(::buildLedger)

    suspend fun currentLedger(): Ledger = buildLedger(dao.getAll())

    suspend fun add(type: MovementType, amountCents: Long, description: String) {
        dao.insert(Movement(type = type, amountCents = amountCents, description = description.trim()))
        refreshWidget()
    }

    suspend fun edit(id: Long, amountCents: Long, description: String) {
        val current = dao.get(id) ?: return
        dao.update(current.copy(amountCents = amountCents, description = description.trim()))
        refreshWidget()
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
        refreshWidget()
    }

    /**
     * Marca un egreso como pagado. Con [PaidWith.INCOME] solo se permite si los
     * ingresos disponibles alcanzan para cubrirlo.
     */
    suspend fun markPaid(id: Long, paidWith: PaidWith): Boolean {
        val expense = dao.get(id) ?: return false
        if (expense.type != MovementType.EXPENSE || expense.isPaid) return false
        if (paidWith == PaidWith.INCOME && expense.amountCents > currentLedger().availableIncome) {
            return false
        }
        dao.update(expense.copy(paidWith = paidWith, paidAt = System.currentTimeMillis()))
        refreshWidget()
        return true
    }

    /** Devuelve un egreso pagado a la lista de pendientes. */
    suspend fun undoPaid(id: Long) {
        val expense = dao.get(id) ?: return
        dao.update(expense.copy(paidWith = null, paidAt = null))
        refreshWidget()
    }

    private suspend fun refreshWidget() {
        FinanzasWidget().updateAll(context)
    }

    companion object {
        @Volatile
        private var instance: FinanzasRepository? = null

        fun get(context: Context): FinanzasRepository =
            instance ?: synchronized(this) {
                instance ?: FinanzasRepository(context.applicationContext).also { instance = it }
            }
    }
}
