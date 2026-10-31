package com.finanzas.app.data

/**
 * Un egreso pendiente tal como se muestra en el widget.
 *
 * @property suggested la app sugiere pagarlo: los ingresos alcanzan yendo del más pequeño
 *   al más grande (se tacha en verde).
 * @property canPay los ingresos disponibles alcanzan para pagar este egreso por sí solo,
 *   aunque no esté sugerido. Solo así se puede marcar el visto.
 * @property shortfall cuánto falta para poder pagarlo (0 si [canPay]).
 */
data class ExpenseRow(
    val expense: Movement,
    val suggested: Boolean,
    val canPay: Boolean,
    val shortfall: Long,
)

data class Ledger(
    /** Egresos pendientes del más pequeño al más grande. */
    val pending: List<ExpenseRow>,
    /** Ingresos ya recibidos, del más reciente al más antiguo. */
    val incomes: List<Movement>,
    /** Ingresos por cobrar, del que llega primero al último. */
    val toReceive: List<Movement>,
    /** Egresos ya pagados y gastos rápidos, el más reciente primero. */
    val paid: List<Movement>,
    val totalIncome: Long,
    val spentFromIncome: Long,
    val pendingExpenses: Long,
    val toReceiveTotal: Long,
) {
    /** Ingresos recibidos menos lo que ya se pagó con ellos. */
    val availableIncome: Long get() = totalIncome - spentFromIncome
    val suggestedCount: Int get() = pending.count { it.suggested }

    /** Lo último que se ha escrito en gastos, para ofrecerlo como atajo. */
    val recentDescriptions: List<String>
        get() = paid.map { it.description.trim() }.filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }.take(8)

    companion object {
        val EMPTY = buildLedger(emptyList())
    }
}

/**
 * Ordena los egresos pendientes de menor a mayor y va "tachando" como sugerencia los que
 * se alcanzan a pagar con los ingresos disponibles, empezando por los más pequeños. Aparte,
 * cualquier egreso que quepa en los ingresos disponibles se puede pagar.
 */
fun buildLedger(movements: List<Movement>): Ledger {
    val expenses = movements.filter { it.type == MovementType.EXPENSE }
    val allIncomes = movements.filter { it.type == MovementType.INCOME }
    val received = allIncomes.filter { !it.isToReceive }
    val toReceive = allIncomes.filter { it.isToReceive }
        .sortedWith(compareBy<Movement> { it.expectedEpochDay }.thenBy { it.createdAt })
    val pending = expenses.filter { !it.isPaid }
        .sortedWith(compareBy<Movement> { it.amountCents }.thenBy { it.createdAt })
    val paid = expenses.filter { it.isPaid }.sortedByDescending { it.paidAt ?: 0 }

    val totalIncome = received.sumOf { it.amountCents }
    val spentFromIncome = paid.filter { it.paidWith == PaidWith.INCOME || it.paidWith == PaidWith.SPENT }
        .sumOf { it.amountCents }
    val available = totalIncome - spentFromIncome

    var remaining = available
    val rows = pending.map { expense ->
        val suggested = expense.amountCents <= remaining
        if (suggested) remaining -= expense.amountCents
        val canPay = expense.amountCents <= available
        ExpenseRow(
            expense = expense,
            suggested = suggested,
            canPay = canPay,
            shortfall = if (canPay) 0 else expense.amountCents - available.coerceAtLeast(0),
        )
    }

    return Ledger(
        pending = rows,
        incomes = received.sortedByDescending { it.createdAt },
        toReceive = toReceive,
        paid = paid,
        totalIncome = totalIncome,
        spentFromIncome = spentFromIncome,
        pendingExpenses = pending.sumOf { it.amountCents },
        toReceiveTotal = toReceive.sumOf { it.amountCents },
    )
}

/** Lo gastado en un motivo dentro de un periodo, para "¿En qué gastas más?". */
data class SpendingGroup(val description: String, val totalCents: Long, val count: Int)

/**
 * Agrupa los gastos por motivo (sin importar mayúsculas ni espacios), del que más
 * dinero se lleva al que menos.
 */
fun groupSpending(spending: List<Movement>): List<SpendingGroup> =
    spending.groupBy { it.description.trim().lowercase() }
        .map { (_, items) ->
            SpendingGroup(
                description = items.first().description.trim().ifEmpty { "Sin motivo" },
                totalCents = items.sumOf { it.amountCents },
                count = items.size,
            )
        }
        .sortedByDescending { it.totalCents }
