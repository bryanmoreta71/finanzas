package com.finanzas.app.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import com.finanzas.app.MainActivity
import com.finanzas.app.R
import com.finanzas.app.data.ExpenseRow
import com.finanzas.app.data.Ledger
import com.finanzas.app.data.Movement
import com.finanzas.app.data.formatMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val Prompt = "C:\\finanzas>"
private const val WindowTitle = "C:\\WINDOWS\\system32\\cmd.exe"
private val cmdDate = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", Locale.forLanguageTag("es"))
private val cmdShortDate = DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("es"))

private val TAmount = 64.dp
private val TIncome = 80.dp
private val TPay = 44.dp

private const val IntroId = Long.MIN_VALUE + 10
private const val ExpensesCmdId = Long.MIN_VALUE + 11
private const val EmptyId = Long.MIN_VALUE + 12
private const val ToReceiveCmdId = Long.MIN_VALUE + 13

/**
 * El widget dibujado como una ventana de consola: barra de título, prompt, comandos
 * y la tabla de egresos como si fuera la salida de un programa.
 */
@Composable
internal fun TerminalContent(ledger: Ledger, today: LocalDate, look: Look, preview: Boolean) {
    val style = look.style
    val toReceive = if (style.showToReceive) ledger.toReceive else emptyList()
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(look.background)
            .cornerRadius(style.cornerRadius.dp),
    ) {
        TitleBar(look, preview)
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        ) {
            if (preview) {
                // Las columnas de Glance admiten hasta 10 hijos; cada bloque cuenta como uno.
                Column(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    Intro(look, today)
                    ExpensesCommand(look)
                    val max = if (toReceive.isEmpty()) 6 else 3
                    if (ledger.pending.isEmpty()) EmptyLine(look)
                    ledger.pending.take(max).forEach { ExpenseLine(look, it, interactive = false) }
                    if (toReceive.isNotEmpty()) {
                        Column {
                            Command(look, "porcobrar")
                            ToReceiveLine(look, toReceive.first(), interactive = false)
                        }
                    }
                }
            } else {
                LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    item(itemId = IntroId) { Intro(look, today) }
                    item(itemId = ExpensesCmdId) { ExpensesCommand(look) }
                    if (ledger.pending.isEmpty()) item(itemId = EmptyId) { EmptyLine(look) }
                    items(ledger.pending.size, itemId = { ledger.pending[it].expense.id }) { i ->
                        ExpenseLine(look, ledger.pending[i], interactive = true)
                    }
                    if (toReceive.isNotEmpty()) {
                        item(itemId = ToReceiveCmdId) { Command(look, "porcobrar") }
                        items(toReceive.size, itemId = { -toReceive[it].id }) { i ->
                            ToReceiveLine(look, toReceive[i], interactive = true)
                        }
                    }
                }
            }
            Footer(look, ledger)
        }
    }
}

@Composable
private fun TitleBar(look: Look, preview: Boolean) {
    val openApp = if (preview) GlanceModifier else GlanceModifier.clickable(actionStartActivity<MainActivity>())
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(look.titleBar)
            .padding(start = 8.dp, end = 6.dp, top = 5.dp, bottom = 5.dp)
            .then(openApp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Iconito de la consola: un cuadro negro con "C:\".
        Box(
            modifier = GlanceModifier
                .background(Color.Black)
                .cornerRadius(2.dp)
                .padding(horizontal = 2.dp, vertical = 1.dp),
        ) {
            Text("C:\\", style = look.text(8f, Color.White))
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(
            look.style.title.trim().ifEmpty { WindowTitle },
            style = look.text(11f),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
        )
        Image(
            provider = ImageProvider(R.drawable.ic_settings),
            contentDescription = "Configurar",
            colorFilter = ColorFilter.tint(ColorProvider(look.secondary)),
            modifier = GlanceModifier.size(16.dp),
        )
        Spacer(GlanceModifier.width(10.dp))
        Text("─   ☐   ✕", style = look.text(11f, look.secondary), maxLines = 1)
    }
}

@Composable
private fun Intro(look: Look, today: LocalDate) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Text("Microsoft Windows [Versión 10.0.22631]", style = look.text(11f, look.secondary), maxLines = 1)
        Spacer(GlanceModifier.height(4.dp))
        Text("${Prompt}date /t", style = look.text(12f), maxLines = 1)
        Text(today.format(cmdDate), style = look.text(12f, bold = true), maxLines = 1)
    }
}

@Composable
private fun Command(look: Look, command: String) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Spacer(GlanceModifier.height(3.dp))
        Text(Prompt + command, style = look.text(12f), maxLines = 1)
    }
}

/** El comando de egresos con los títulos de columna subrayados con guiones, como `dir`. */
@Composable
private fun ExpensesCommand(look: Look) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Command(look, "egresos /orden:monto")
        if (look.style.showHeaders) {
            TableRow(look, "MONTO", "MOTIVO", "INGRESOS", "PAGAR")
            TableRow(look, "-----", "------", "--------", "-----")
        }
    }
}

@Composable
private fun TableRow(look: Look, a: String, b: String, c: String, d: String) {
    val st = look.text(11f, look.secondary)
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        Text(a, style = st, modifier = GlanceModifier.width(look.scaled(TAmount)), maxLines = 1)
        Text(b, style = st, modifier = GlanceModifier.defaultWeight(), maxLines = 1)
        Text(c, style = st, modifier = GlanceModifier.width(look.scaled(TIncome)), maxLines = 1)
        Text(d, style = look.text(11f, look.secondary, align = TextAlign.End), modifier = GlanceModifier.width(TPay), maxLines = 1)
    }
}

@Composable
private fun EmptyLine(look: Look) {
    Text("No hay egresos pendientes.", style = look.text(12f, look.secondary), maxLines = 1)
}

@Composable
private fun ExpenseLine(look: Look, row: ExpenseRow, interactive: Boolean) {
    val expense = row.expense
    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatMoney(expense.amountCents),
            style = look.text(13f, bold = look.style.boldAmounts),
            modifier = GlanceModifier.width(look.scaled(TAmount)),
            maxLines = 1,
        )
        Text(expense.description, style = look.text(13f), modifier = GlanceModifier.defaultWeight(), maxLines = 1)
        Box(modifier = GlanceModifier.width(look.scaled(TIncome))) {
            when {
                row.suggested -> Text(
                    formatMoney(expense.amountCents),
                    style = look.text(13f, look.accent, bold = true, strike = true),
                    maxLines = 1,
                )
                row.canPay -> Text("ALCANZA", style = look.text(11f, look.secondary), maxLines = 1)
                else -> Text("FALTA ${formatMoney(row.shortfall)}", style = look.text(11f, look.expense), maxLines = 1)
            }
        }
        Box(modifier = GlanceModifier.width(TPay), contentAlignment = Alignment.CenterEnd) {
            if (row.canPay) {
                Text(
                    "[ ]",
                    style = look.text(14f, if (row.suggested) look.accent else look.text, bold = true),
                    modifier = GlanceModifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .then(if (interactive) GlanceModifier.clickable(action<PayExpenseAction>(expense.id)) else GlanceModifier),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ToReceiveLine(look: Look, income: Movement, interactive: Boolean) {
    val day = income.expectedEpochDay?.let { LocalDate.ofEpochDay(it).format(cmdShortDate) } ?: ""
    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatMoney(income.amountCents),
            style = look.text(13f, look.accent, bold = look.style.boldAmounts),
            modifier = GlanceModifier.width(look.scaled(TAmount)),
            maxLines = 1,
        )
        Text(income.description.ifBlank { "Ingreso" }, style = look.text(13f), modifier = GlanceModifier.defaultWeight(), maxLines = 1)
        Text(day, style = look.text(12f, look.secondary), maxLines = 1)
        Spacer(GlanceModifier.width(6.dp))
        Text(
            "[LLEGÓ]",
            style = look.text(12f, look.accent, bold = true),
            modifier = GlanceModifier
                .padding(vertical = 2.dp)
                .then(if (interactive) GlanceModifier.clickable(action<ReceiveIncomeAction>(income.id)) else GlanceModifier),
            maxLines = 1,
        )
    }
}

@Composable
private fun Footer(look: Look, ledger: Ledger) {
    Column(modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)) {
        if (look.style.showTotals) {
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                Text("EGRESOS ", style = look.text(12f, look.secondary), maxLines = 1)
                Text(formatMoney(ledger.pendingExpenses), style = look.text(12f, look.expense, bold = true), maxLines = 1)
                Spacer(GlanceModifier.defaultWeight())
                Text("INGRESOS ", style = look.text(12f, look.secondary), maxLines = 1)
                Text(formatMoney(ledger.availableIncome), style = look.text(12f, look.accent, bold = true), maxLines = 1)
            }
        }
        Text("${Prompt}_", style = look.text(12f), maxLines = 1)
    }
}
