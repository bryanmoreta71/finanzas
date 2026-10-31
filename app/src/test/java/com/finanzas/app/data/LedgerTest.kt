package com.finanzas.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LedgerTest {
    private var nextId = 1L
    private fun expense(cents: Long, paidWith: PaidWith? = null) = Movement(
        id = nextId++, type = MovementType.EXPENSE, amountCents = cents, description = "e$cents",
        createdAt = nextId, paidWith = paidWith, paidAt = paidWith?.let { nextId },
    )
    private fun income(cents: Long) = Movement(
        id = nextId++, type = MovementType.INCOME, amountCents = cents, description = "i", createdAt = nextId,
    )

    @Test
    fun pendingSortedFromSmallestToLargest() {
        val ledger = buildLedger(listOf(expense(30000), expense(5000), expense(12000)))
        assertEquals(listOf(5000L, 12000L, 30000L), ledger.pending.map { it.expense.amountCents })
    }

    @Test
    fun incomesCoverSmallestExpensesFirst() {
        val ledger = buildLedger(listOf(income(20000), expense(5000), expense(12000), expense(30000)))
        assertEquals(listOf(true, true, false), ledger.pending.map { it.covered })
        assertEquals(3000L, ledger.pending[1].remainingAfter)
        assertEquals(27000L, ledger.pending[2].shortfall)
        assertEquals(47000L, ledger.pendingExpenses)
    }

    @Test
    fun payingWithIncomeSubtractsFromIncome() {
        val ledger = buildLedger(listOf(income(20000), expense(5000, PaidWith.INCOME), expense(12000)))
        assertEquals(15000L, ledger.availableIncome)
        assertEquals(12000L, ledger.pendingExpenses)
        assertEquals(1, ledger.paid.size)
    }

    @Test
    fun payingWithOtherSourceKeepsIncome() {
        val ledger = buildLedger(listOf(income(20000), expense(5000, PaidWith.OTHER), expense(12000)))
        assertEquals(20000L, ledger.availableIncome)
        assertEquals(12000L, ledger.pendingExpenses)
    }

    @Test
    fun moneyParsingAndFormatting() {
        assertEquals(5000L, parseMoney("50"))
        assertEquals(5050L, parseMoney("50,5"))
        assertEquals(125075L, parseMoney("1,250.75"))
        assertEquals(2000L, parseMoney("$ 20"))
        assertNull(parseMoney("-3"))
        assertNull(parseMoney("abc"))
        assertEquals("$50", formatMoney(5000))
        assertEquals("$1,250.75", formatMoney(125075))
        assertEquals("-$3", formatMoney(-300))
    }
}
