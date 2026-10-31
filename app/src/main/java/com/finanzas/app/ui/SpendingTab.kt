package com.finanzas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzas.app.data.Ledger
import com.finanzas.app.data.Movement
import com.finanzas.app.data.PaidWith
import com.finanzas.app.data.formatMoney
import com.finanzas.app.data.groupSpending
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class SpendingPeriod(val label: String) {
    TODAY("Hoy"),
    WEEK("Semana"),
    MONTH("Mes"),
    ALL("Todo"),
    ;

    /** Primer día que entra en el periodo (null = sin límite). */
    fun start(today: LocalDate): LocalDate? = when (this) {
        TODAY -> today
        WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        MONTH -> today.withDayOfMonth(1)
        ALL -> null
    }
}

private val spanish = Locale.forLanguageTag("es")
private val dayTitle = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", spanish)
private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", spanish)

private fun Movement.day(zone: ZoneId): LocalDate = Instant.ofEpochMilli(paidAt ?: createdAt).atZone(zone).toLocalDate()

/** "¿En qué he gastado?": totales del periodo, lo que más se lleva la plata y la lista por día. */
@Composable
fun SpendingTab(ledger: Ledger, onUndo: (Long) -> Unit) {
    var period by rememberSaveable { mutableStateOf(SpendingPeriod.WEEK) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val start = period.start(today)
    val spending = ledger.paid.filter { start == null || !it.day(zone).isBefore(start) }
    val total = spending.sumOf { it.amountCents }
    val groups = groupSpending(spending)
    val byDay = spending.groupBy { it.day(zone) }.toSortedMap(compareByDescending { it })

    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SpendingPeriod.entries) { p ->
                    FilterChip(selected = p == period, onClick = { period = p }, label = { Text(p.label) })
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        when (period) {
                            SpendingPeriod.TODAY -> "Hoy gastaste"
                            SpendingPeriod.WEEK -> "Esta semana gastaste"
                            SpendingPeriod.MONTH -> "Este mes gastaste"
                            SpendingPeriod.ALL -> "En total gastaste"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        formatMoney(total),
                        style = MaterialTheme.typography.headlineMedium,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (spending.size == 1) "en 1 gasto" else "en ${spending.size} gastos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (spending.isEmpty()) {
            item {
                EmptyMessage(
                    "No hay gastos en este periodo.\nAnótalos con el botón \"Anotar gasto\" o con el widget de gasto rápido.",
                )
            }
            return@LazyColumn
        }

        item { SectionHeader("¿En qué gastas más?") }
        val top = groups.take(6)
        val max = top.first().totalCents.coerceAtLeast(1)
        items(top, key = { "g-" + it.description.lowercase() }) { group ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(group.description, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (group.count == 1) "1 vez  " else "${group.count} veces  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(formatMoney(group.totalCents), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(group.totalCents.toFloat() / max)
                            .height(8.dp)
                            .background(ExpenseRed.copy(alpha = 0.75f), RoundedCornerShape(4.dp)),
                    )
                }
            }
        }

        item { SectionHeader("Día por día") }
        byDay.forEach { (day, items) ->
            item(key = "d-$day") {
                val name = when (day) {
                    today -> "Hoy"
                    today.minusDays(1) -> "Ayer"
                    else -> day.format(dayTitle).replaceFirstChar { it.uppercase() }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Text(name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Text(formatMoney(items.sumOf { it.amountCents }), style = MaterialTheme.typography.labelLarge, color = ExpenseRed)
                }
            }
            items(items.sortedByDescending { it.paidAt ?: it.createdAt }, key = { it.id }) { movement ->
                SpendingItem(movement, zone, onUndo)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SpendingItem(movement: Movement, zone: ZoneId, onUndo: (Long) -> Unit) {
    val time = Instant.ofEpochMilli(movement.paidAt ?: movement.createdAt).atZone(zone).format(timeFormat)
    val (label, color) = when (movement.paidWith) {
        PaidWith.SPENT -> "Gasto" to MaterialTheme.colorScheme.onSurfaceVariant
        PaidWith.INCOME -> "Egreso pagado con ingresos" to IncomeGreen
        else -> "Pagado con otra fuente" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(movement.description.ifBlank { "Sin motivo" }, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.bodySmall, color = color)
        }
        Text(formatMoney(movement.amountCents), fontWeight = FontWeight.Bold)
        TextButton(onClick = { onUndo(movement.id) }) {
            Text(if (movement.paidWith == PaidWith.SPENT) "Borrar" else "Deshacer")
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}
