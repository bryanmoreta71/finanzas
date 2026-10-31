package com.finanzas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzas.app.data.ExpenseRow
import com.finanzas.app.data.Ledger
import com.finanzas.app.data.Movement
import com.finanzas.app.data.MovementType
import com.finanzas.app.data.PaidWith
import com.finanzas.app.data.centsToInput
import com.finanzas.app.data.formatMoney
import com.finanzas.app.data.parseMoney
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
fun FinanzasScreen(viewModel: FinanzasViewModel = viewModel()) {
    val ledger by viewModel.ledger.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var dialog by remember { mutableStateOf<DialogState?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis finanzas") }) },
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
                    onPay = { id, with -> viewModel.markPaid(id, with) },
                    onEdit = { dialog = DialogState.Edit(it) },
                )
                1 -> IncomesTab(ledger = ledger, onEdit = { dialog = DialogState.Edit(it) })
                else -> PaidTab(ledger = ledger, onUndo = viewModel::undoPaid)
            }
        }
    }

    when (val d = dialog) {
        is DialogState.New -> MovementDialog(
            initialType = d.type,
            initial = null,
            onDismiss = { dialog = null },
            onSave = { type, cents, text -> viewModel.add(type, cents, text); dialog = null },
            onDelete = null,
        )
        is DialogState.Edit -> MovementDialog(
            initialType = d.movement.type,
            initial = d.movement,
            onDismiss = { dialog = null },
            onSave = { _, cents, text -> viewModel.edit(d.movement.id, cents, text); dialog = null },
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
            if (ledger.pending.isNotEmpty()) {
                Text(
                    "Con tus ingresos alcanzas a pagar ${ledger.coveredCount} de ${ledger.pending.size} egresos",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ExpensesTab(
    ledger: Ledger,
    onPay: (Long, PaidWith) -> Unit,
    onEdit: (Movement) -> Unit,
) {
    if (ledger.pending.isEmpty()) {
        EmptyMessage("No tienes egresos pendientes.\nToca \"Añadir egreso\" para agregar uno.")
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val style = MaterialTheme.typography.labelSmall
                Text("Otra\nfuente", style = style, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
                Text("Monto", style = style, modifier = Modifier.width(80.dp))
                Text("Motivo / persona", style = style, modifier = Modifier.weight(1f))
                Text("Ingresos", style = style, modifier = Modifier.width(88.dp), textAlign = TextAlign.Center)
                Text("Pagar con\ningresos", style = style, modifier = Modifier.width(48.dp), textAlign = TextAlign.Center)
            }
            HorizontalDivider()
        }
        items(ledger.pending, key = { it.expense.id }) { row ->
            ExpenseItem(row, onPay, onEdit)
            HorizontalDivider()
        }
        item {
            Text(
                "Toca un egreso para editarlo o eliminarlo. Visto izquierdo: lo pagaste con otra fuente. " +
                    "Visto derecho: lo pagas con tus ingresos y se resta de ellos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ExpenseItem(row: ExpenseRow, onPay: (Long, PaidWith) -> Unit, onEdit: (Movement) -> Unit) {
    val expense = row.expense
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onEdit(expense) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Checkbox(checked = false, onCheckedChange = { onPay(expense.id, PaidWith.OTHER) })
        }
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
                    if (row.covered) {
                        Modifier.background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (row.covered) {
                Text(
                    formatMoney(expense.amountCents),
                    color = IncomeGreen,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.LineThrough,
                )
            } else {
                Text(
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
                enabled = row.covered,
                onCheckedChange = { onPay(expense.id, PaidWith.INCOME) },
                colors = CheckboxDefaults.colors(uncheckedColor = IncomeGreen, checkedColor = IncomeGreen),
            )
        }
    }
}

@Composable
private fun IncomesTab(ledger: Ledger, onEdit: (Movement) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TotalLine("Total de ingresos", ledger.totalIncome)
                TotalLine("Usado para pagar egresos", -ledger.spentFromIncome)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                TotalLine("Disponible", ledger.availableIncome, bold = true)
            }
            HorizontalDivider()
        }
        if (ledger.incomes.isEmpty()) {
            item { EmptyMessage("Aún no tienes ingresos.\nToca \"Añadir ingreso\" para agregar uno.") }
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
        EmptyMessage("Aquí aparecerán los egresos que vayas marcando como pagados.")
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
                TextButton(onClick = { onUndo(expense.id) }) { Text("Deshacer") }
            }
            HorizontalDivider()
        }
    }
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
private fun EmptyMessage(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MovementDialog(
    initialType: MovementType,
    initial: Movement?,
    onDismiss: () -> Unit,
    onSave: (MovementType, Long, String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var type by remember { mutableStateOf(initialType) }
    var amount by remember { mutableStateOf(initial?.let { centsToInput(it.amountCents) } ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    val cents = parseMoney(amount)
    val isExpense = type == MovementType.EXPENSE

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    initial == null && isExpense -> "Nuevo egreso"
                    initial == null -> "Nuevo ingreso"
                    isExpense -> "Editar egreso"
                    else -> "Editar ingreso"
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initial == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = isExpense,
                            onClick = { type = MovementType.EXPENSE },
                            label = { Text("Egreso") },
                        )
                        FilterChip(
                            selected = !isExpense,
                            onClick = { type = MovementType.INCOME },
                            label = { Text("Ingreso") },
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monto") },
                    prefix = { Text("$") },
                    singleLine = true,
                    isError = amount.isNotBlank() && cents == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isExpense) "Motivo o persona" else "Descripción (opcional)") },
                    singleLine = true,
                )
                if (onDelete != null) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onDelete) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = cents != null && (!isExpense || description.isNotBlank()),
                onClick = { cents?.let { onSave(type, it, description) } },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
