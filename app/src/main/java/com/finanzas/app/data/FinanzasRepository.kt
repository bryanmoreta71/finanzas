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

    /** [expectedEpochDay] solo aplica a ingresos: si no es null, el ingreso queda "por cobrar". */
    suspend fun add(type: MovementType, amountCents: Long, description: String, expectedEpochDay: Long?) {
        dao.insert(
            Movement(
                type = type,
                amountCents = amountCents,
                description = description.trim(),
                expectedEpochDay = expectedEpochDay.takeIf { type == MovementType.INCOME },
            ),
        )
        refreshWidget()
    }

    suspend fun edit(id: Long, amountCents: Long, description: String, expectedEpochDay: Long?) {
        val current = dao.get(id) ?: return
        dao.update(
            current.copy(
                amountCents = amountCents,
                description = description.trim(),
                expectedEpochDay = expectedEpochDay.takeIf { current.type == MovementType.INCOME },
            ),
        )
        refreshWidget()
    }

    /** Un ingreso por cobrar ya llegó: pasa a contar en los ingresos disponibles. */
    suspend fun markReceived(id: Long) {
        val income = dao.get(id) ?: return
        if (!income.isToReceive) return
        dao.update(income.copy(expectedEpochDay = null, createdAt = System.currentTimeMillis()))
        refreshWidget()
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
        refreshWidget()
    }

    /**
     * Paga un egreso con los ingresos. Solo se permite si los ingresos disponibles
     * alcanzan para cubrirlo, sea o no uno de los sugeridos.
     */
    suspend fun payWithIncome(id: Long): Boolean {
        val expense = dao.get(id) ?: return false
        if (expense.type != MovementType.EXPENSE || expense.isPaid) return false
        if (expense.amountCents > currentLedger().availableIncome) return false
        dao.update(expense.copy(paidWith = PaidWith.INCOME, paidAt = System.currentTimeMillis()))
        refreshWidget()
        return true
    }

    /** Devuelve un egreso pagado a la lista de pendientes. */
    suspend fun undoPaid(id: Long) {
        val expense = dao.get(id) ?: return
        dao.update(expense.copy(paidWith = null, paidAt = null))
        refreshWidget()
    }

    suspend fun refreshWidget() {
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
