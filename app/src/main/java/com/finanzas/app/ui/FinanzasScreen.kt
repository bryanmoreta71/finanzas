package com.finanzas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanzas.app.data.ExpenseRow
import com.finanzas.app.data.Ledger
import com.finanzas.app.data.Movement
import com.finanzas.app.data.MovementType
import com.finanzas.app.data.PaidWith
import com.finanzas.app.data.formatLongDay
import com.finanzas.app.data.formatMoney
import com.finanzas.app.data.relativeDay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val shortDate = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es"))

/** Qué diálogo está abierto: uno nuevo de un tipo, o la edición de un movimiento. */
private sealed interface DialogState {
    data class New(val type: MovementType) : DialogState
    data class Edit(val movement: Movement) : DialogState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanzasScreen(
    ledger: Ledger,
    viewModel: FinanzasViewModel,
    onCustomizeWidget: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<DialogState?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis finanzas") },
                actions = {
                    TextButton(onClick = onCustomizeWidget) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text("  Widget")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    dialog = DialogState.New(if (tab == 1) MovementType.INCOME else MovementType.EXPENSE)
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(if (tab == 1) "Añadir ingreso" else "Añadir egreso") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SummaryCard(ledger)
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Egresos") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Ingresos") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Pagados") })
            }
            when (tab) {
                0 -> ExpensesTab(
                    ledger = ledger,
                    onPay = viewModel::payWithIncome,
                    onEdit = { dialog = DialogState.Edit(it) },
                )
                1 -> IncomesTab(
                    ledger = ledger,
                    onEdit = { dialog = DialogState.Edit(it) },
                    onReceived = viewModel::markReceived,
                )
                else -> PaidTab(ledger = ledger, onUndo = viewModel::undoPaid)
            }
        }
    }

    when (val d = dialog) {
        is DialogState.New -> MovementDialog(
            initialType = d.type,
            initial = null,
            onDismiss = { dialog = null },
            onSave = { type, cents, text, day -> viewModel.add(type, cents, text, day); dialog = null },
            onDelete = null,
        )
        is DialogState.Edit -> MovementDialog(
            initialType = d.movement.type,
            initial = d.movement,
            onDismiss = { dialog = null },
            onSave = { _, cents, text, day -> viewModel.edit(d.movement.id, cents, text, day); dialog = null },
            onDelete = { viewModel.delete(d.movement.id); dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun SummaryCard(ledger: Ledger) {
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Egresos pendientes", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatMoney(ledger.pendingExpenses),
                        style = MaterialTheme.typography.headlineSmall,
                        color = ExpenseRed,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Ingresos disponibles", style = MaterialTheme.typography.labelMedium)
                    Text(
                        formatMoney(ledger.availableIncome),
                        style = MaterialTheme.typography.headlineSmall,
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (ledger.toReceiveTotal > 0) {
                val next = ledger.toReceive.first().expectedEpochDay ?: 0
                Text(
                    "Por cobrar: ${formatMoney(ledger.toReceiveTotal)} · el próximo llega ${relativeDay(next)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (ledger.pending.isNotEmpty()) {
                Text(
                    "Sugerencia: con tus ingresos alcanzas a pagar ${ledger.suggestedCount} de ${ledger.pending.size} egresos",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    ledger: Ledger,
    onPay: (Long) -> Unit,
    onEdit: (Movement) -> Unit,
) {
    if (ledger.pending.isEmpty()) {
        EmptyMessage("No tienes egresos pendientes.\nToca \"Añadir egreso\" para agregar uno.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val style = MaterialTheme.typography.labelSmall
                Text("Monto", style = style, modifier = Modifier.width(80.dp))
                Text("Motivo / persona", style = style, modifier = Modifier.weight(1f))
                Text("Ingresos", style = style, modifier = Modifier.width(88.dp), textAlign = TextAlign.Center)
                Text("Pagar", style = style, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
            }
            HorizontalDivider()
        }
        items(ledger.pending, key = { it.expense.id }) { row ->
            ExpenseItem(row, onPay, onEdit)
            HorizontalDivider()
        }
        item {
            Text(
                "En verde, lo que te sugiere la app pagar empezando por lo más pequeño. Igual puedes " +
                    "marcar el visto de cualquier egreso que te alcance con tus ingresos; se resta de ellos. " +
                    "Toca un egreso para editarlo o eliminarlo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ExpenseItem(row: ExpenseRow, onPay: (Long) -> Unit, onEdit: (Movement) -> Unit) {
    val expense = row.expense
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onEdit(expense) }
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatMoney(expense.amountCents),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp),
            maxLines = 1,
        )
        Text(
            expense.description,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier
                .width(88.dp)
                .padding(4.dp)
                .then(
                    if (row.suggested) {
                        Modifier.background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                row.suggested -> Text(
                    formatMoney(expense.amountCents),
                    color = IncomeGreen,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.LineThrough,
                )
                row.canPay -> Text(
                    "Alcanza",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> Text(
                    "Faltan ${formatMoney(row.shortfall)}",
                    color = ExpenseRed,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Checkbox(
                checked = false,
                enabled = row.canPay,
                onCheckedChange = { onPay(expense.id) },
                colors = CheckboxDefaults.colors(
                    uncheckedColor = if (row.suggested) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedColor = IncomeGreen,
                ),
            )
        }
    }
}

@Composable
private fun IncomesTab(ledger: Ledger, onEdit: (Movement) -> Unit, onReceived: (Long) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TotalLine("Total recibido", ledger.totalIncome)
                TotalLine("Usado para pagar egresos", -ledger.spentFromIncome)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                TotalLine("Disponible", ledger.availableIncome, bold = true)
                if (ledger.toReceiveTotal > 0) TotalLine("Por cobrar", ledger.toReceiveTotal)
            }
            HorizontalDivider()
        }
        if (ledger.toReceive.isNotEmpty()) {
            item { SectionTitle("Por cobrar") }
            items(ledger.toReceive, key = { it.id }) { income ->
                val day = income.expectedEpochDay ?: 0
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(income) }
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(income.description.ifBlank { "Ingreso" })
                        Text(
                            "${formatLongDay(day)} · ${relativeDay(day)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(formatMoney(income.amountCents), color = IncomeGreen.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                    TextButton(onClick = { onReceived(income.id) }) { Text("Llegó") }
                }
                HorizontalDivider()
            }
            item { SectionTitle("Recibidos") }
        }
        if (ledger.incomes.isEmpty()) {
            item { EmptyMessage("Aún no tienes ingresos recibidos.\nToca \"Añadir ingreso\" para agregar uno.") }
        }
        items(ledger.incomes, key = { it.id }) { income ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onEdit(income) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(income.description.ifBlank { "Ingreso" })
                    Text(
                        shortDate.format(Date(income.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(formatMoney(income.amountCents), color = IncomeGreen, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PaidTab(ledger: Ledger, onUndo: (Long) -> Unit) {
    if (ledger.paid.isEmpty()) {
        EmptyMessage("Aquí aparecerán los egresos que vayas pagando con tus ingresos.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        items(ledger.paid, key = { it.id }) { expense ->
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${formatMoney(expense.amountCents)}  ${expense.description}",
                        textDecoration = TextDecoration.LineThrough,
                    )
                    val source = if (expense.paidWith == PaidWith.INCOME) "Con ingresos" else "Otra fuente"
                    val date = expense.paidAt?.let { " · " + shortDate.format(Date(it)) } ?: ""
                    Text(
                        source + date,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (expense.paidWith == PaidWith.INCOME) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FilledTonalButton(onClick = { onUndo(expense.id) }) { Text("Deshacer") }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun TotalLine(label: String, cents: Long, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else null)
        Text(
            formatMoney(cents),
            fontWeight = if (bold) FontWeight.Bold else null,
            color = if (bold) IncomeGreen else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun EmptyMessage(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
