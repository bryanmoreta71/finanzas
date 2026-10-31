package com.finanzas.app.data

/**
 * Un egreso pendiente tal como se muestra en el widget.
 *
 * @property covered si los ingresos disponibles alcanzan para pagarlo (se tacha en verde).
 * @property remainingAfter ingresos que quedan después de cubrir este egreso y los anteriores.
 * @property shortfall cuánto falta para poder cubrirlo (0 si está cubierto).
 */
data class ExpenseRow(
    val expense: Movement,
    val covered: Boolean,
    val remainingAfter: Long,
    val shortfall: Long,
)

data class Ledger(
    /** Egresos pendientes del más pequeño al más grande. */
    val pending: List<ExpenseRow>,
    /** Ingresos del más reciente al más antiguo. */
    val incomes: List<Movement>,
    /** Egresos ya pagados, el último pagado primero. */
    val paid: List<Movement>,
    val totalIncome: Long,
    val spentFromIncome: Long,
    val pendingExpenses: Long,
) {
    /** Ingresos menos lo que ya se pagó con ellos (visto derecho). */
    val availableIncome: Long get() = totalIncome - spentFromIncome
    val coveredCount: Int get() = pending.count { it.covered }

    companion object {
        val EMPTY = buildLedger(emptyList())
    }
}

/**
 * Ordena los egresos pendientes de menor a mayor y va "tachando" con los ingresos
 * disponibles los que se pueden pagar, empezando por los más pequeños.
 */
fun buildLedger(movements: List<Movement>): Ledger {
    val expenses = movements.filter { it.type == MovementType.EXPENSE }
    val incomes = movements.filter { it.type == MovementType.INCOME }
    val pending = expenses.filter { !it.isPaid }
        .sortedWith(compareBy<Movement> { it.amountCents }.thenBy { it.createdAt })
    val paid = expenses.filter { it.isPaid }.sortedByDescending { it.paidAt ?: 0 }

    val totalIncome = incomes.sumOf { it.amountCents }
    val spentFromIncome = paid.filter { it.paidWith == PaidWith.INCOME }.sumOf { it.amountCents }

    var remaining = totalIncome - spentFromIncome
    val rows = pending.map { expense ->
        if (expense.amountCents <= remaining) {
            remaining -= expense.amountCents
            ExpenseRow(expense, covered = true, remainingAfter = remaining, shortfall = 0)
        } else {
            ExpenseRow(
                expense,
                covered = false,
                remainingAfter = remaining,
                shortfall = expense.amountCents - remaining.coerceAtLeast(0),
            )
        }
    }

    return Ledger(
        pending = rows,
        incomes = incomes.sortedByDescending { it.createdAt },
        paid = paid,
        totalIncome = totalIncome,
        spentFromIncome = spentFromIncome,
        pendingExpenses = pending.sumOf { it.amountCents },
    )
}
