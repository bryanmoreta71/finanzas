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
    private fun income(cents: Long, expectedEpochDay: Long? = null) = Movement(
        id = nextId++, type = MovementType.INCOME, amountCents = cents, description = "i", createdAt = nextId,
        expectedEpochDay = expectedEpochDay,
    )

    @Test
    fun pendingSortedFromSmallestToLargest() {
        val ledger = buildLedger(listOf(expense(30000), expense(5000), expense(12000)))
        assertEquals(listOf(5000L, 12000L, 30000L), ledger.pending.map { it.expense.amountCents })
    }

    @Test
    fun suggestionCoversSmallestFirst() {
        val ledger = buildLedger(listOf(income(20000), expense(5000), expense(12000), expense(30000)))
        assertEquals(listOf(true, true, false), ledger.pending.map { it.suggested })
        assertEquals(47000L, ledger.pendingExpenses)
    }

    @Test
    fun anyExpenseThatFitsCanBePaidEvenIfNotSuggested() {
        // Ingresos de $250: se sugieren $20 + $35 + $60, pero el de $200 también se puede pagar.
        val ledger = buildLedger(listOf(income(25000), expense(2000), expense(3500), expense(6000), expense(20000), expense(30000)))
        assertEquals(listOf(true, true, true, false, false), ledger.pending.map { it.suggested })
        assertEquals(listOf(true, true, true, true, false), ledger.pending.map { it.canPay })
        assertEquals(5000L, ledger.pending.last().shortfall)
    }

    @Test
    fun payingWithIncomeSubtractsFromIncome() {
        val ledger = buildLedger(listOf(income(25000), expense(20000, PaidWith.INCOME), expense(6000)))
        assertEquals(5000L, ledger.availableIncome)
        assertEquals(6000L, ledger.pendingExpenses)
        assertEquals(false, ledger.pending.single().canPay)
        assertEquals(1000L, ledger.pending.single().shortfall)
    }

    @Test
    fun equalAmountCanBePaid() {
        val ledger = buildLedger(listOf(income(30000), expense(30000)))
        assertEquals(true, ledger.pending.single().canPay)
    }

    @Test
    fun toReceiveIncomeDoesNotCountUntilItArrives() {
        val ledger = buildLedger(listOf(income(10000), income(40000, expectedEpochDay = 20000), income(5000, expectedEpochDay = 19990), expense(20000)))
        assertEquals(10000L, ledger.availableIncome)
        assertEquals(45000L, ledger.toReceiveTotal)
        assertEquals(listOf(19990L, 20000L), ledger.toReceive.map { it.expectedEpochDay })
        assertEquals(false, ledger.pending.single().canPay)
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
        assertEquals("hoy", relativeDay(100, today = 100))
        assertEquals("en 2 días", relativeDay(102, today = 100))
    }
}
