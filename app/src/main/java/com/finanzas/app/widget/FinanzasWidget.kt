package com.finanzas.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.finanzas.app.MainActivity
import com.finanzas.app.R
import com.finanzas.app.data.ExpenseRow
import com.finanzas.app.data.FinanzasRepository
import com.finanzas.app.data.Ledger
import com.finanzas.app.data.Movement
import com.finanzas.app.data.formatMoney
import com.finanzas.app.data.formatShortDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val IdKey = ActionParameters.Key<Long>("movement_id")

private val dateFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es"))

private fun dayLabel(day: LocalDate): String = day.format(dateFormat).replaceFirstChar { it.uppercase() }

class FinanzasWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ledger = FinanzasRepository.get(context).currentLedger()
        val style = WidgetStyleStore.load(context)
        val today = LocalDate.now()
        provideContent {
            WidgetContent(ledger, today, style)
        }
    }
}

class FinanzasWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FinanzasWidget()
}

/** Visto de una fila: pagar el egreso con los ingresos. */
class PayExpenseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[IdKey] ?: return
        FinanzasRepository.get(context).payWithIncome(id)
    }
}

/** Botón "Llegó" de un ingreso por cobrar. */
class ReceiveIncomeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[IdKey] ?: return
        FinanzasRepository.get(context).markReceived(id)
    }
}

/** Colores y tipografía del widget ya resueltos a partir de un [WidgetStyle]. */
internal class Look(val style: WidgetStyle) {
    val text = Color(style.text)
    val secondary = text.copy(alpha = 0.65f)
    val accent = Color(style.accent)
    val accentSoft = accent.copy(alpha = 0.22f)
    val expense = Color(style.expense)
    val row = Color(style.row).copy(alpha = style.rowAlpha)
    val divider = text.copy(alpha = 0.15f)

    /** Barra de título en el diseño de terminal; usa el color de las filas. */
    val titleBar = row
    val background = Color(style.background).copy(alpha = style.backgroundAlpha)
    private val family = FontFamily(style.font.family)

    fun scaled(dp: Dp): Dp = dp * style.textScale

    fun text(
        size: Float,
        color: Color = text,
        bold: Boolean = false,
        strike: Boolean = false,
        align: TextAlign = TextAlign.Start,
    ) = TextStyle(
        color = ColorProvider(color),
        fontSize = (size * style.textScale).sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = family,
        textDecoration = if (strike) TextDecoration.LineThrough else TextDecoration.None,
        textAlign = align,
    )
}

/**
 * El widget completo. Con [preview] = true se dibuja sin acciones y sin lista desplazable,
 * para mostrarlo dentro de la app mientras se personaliza.
 */
@Composable
fun WidgetContent(ledger: Ledger, today: LocalDate, style: WidgetStyle, preview: Boolean = false) {
    val look = Look(style)
    when (style.layout) {
        WidgetLayout.TERMINAL -> return TerminalContent(ledger, today, look, preview)
        WidgetLayout.ANDROID_STUDIO, WidgetLayout.VSCODE -> return IdeContent(ledger, today, look, preview)
        WidgetLayout.NORMAL -> Unit
    }
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(look.background)
            .cornerRadius(style.cornerRadius.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Header(look, dayLabel(today), preview)

        if (style.showHeaders) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Monto", style = look.text(11f, look.secondary), modifier = GlanceModifier.width(look.scaled(AmountWidth)))
                Text("Motivo", style = look.text(11f, look.secondary), modifier = GlanceModifier.defaultWeight())
                Text(
                    "Ingresos",
                    style = look.text(11f, look.secondary, align = TextAlign.Center),
                    modifier = GlanceModifier.width(look.scaled(IncomeWidth)),
                )
                Text(
                    "Pagar",
                    style = look.text(11f, look.secondary, align = TextAlign.End),
                    modifier = GlanceModifier.width(CheckWidth),
                )
            }
        }

        val toReceive = if (style.showToReceive) ledger.toReceive else emptyList()
        if (ledger.pending.isEmpty() && toReceive.isEmpty()) {
            Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Sin egresos pendientes", style = look.text(13f, look.secondary))
            }
        } else if (preview) {
            // Las columnas de Glance admiten hasta 10 hijos.
            Column(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                val expenses = ledger.pending.take(if (toReceive.isEmpty()) 9 else if (style.rowStyle == RowStyle.CARDS) 3 else 4)
                expenses.forEachIndexed { i, row -> ExpenseLine(look, row, i, interactive = false) }
                if (toReceive.isNotEmpty()) {
                    ToReceiveTitle(look)
                    toReceive.take(1).forEachIndexed { i, income -> ToReceiveLine(look, income, i, interactive = false) }
                }
            }
        } else {
            LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                items(ledger.pending.size, itemId = { ledger.pending[it].expense.id }) { i ->
                    ExpenseLine(look, ledger.pending[i], i, interactive = true)
                }
                if (toReceive.isNotEmpty()) {
                    item(itemId = ToReceiveTitleId) { ToReceiveTitle(look) }
                    items(toReceive.size, itemId = { -toReceive[it].id }) { i ->
                        ToReceiveLine(look, toReceive[i], i, interactive = true)
                    }
                }
            }
        }

        if (style.showTotals) {
            Spacer(GlanceModifier.height(4.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Egresos: ${formatMoney(ledger.pendingExpenses)}",
                    style = look.text(13f, look.expense, bold = true),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1,
                )
                Text(
                    "Ingresos: ${formatMoney(ledger.availableIncome)}",
                    style = look.text(13f, look.accent, bold = true, align = TextAlign.End),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1,
                )
            }
        }
    }
}

private val CheckWidth = 40.dp
private val AmountWidth = 64.dp
private val IncomeWidth = 72.dp
private const val ToReceiveTitleId = Long.MIN_VALUE + 1

@Composable
private fun Header(look: Look, today: String, preview: Boolean) {
    val title = look.style.title.trim()
    Row(modifier = GlanceModifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            if (title.isNotEmpty()) {
                Text(title, style = look.text(17f, bold = true), maxLines = 1)
                Text(today, style = look.text(12f, look.secondary), maxLines = 1)
            } else {
                Text(today, style = look.text(16f, bold = true), maxLines = 1)
            }
        }
        Image(
            provider = ImageProvider(R.drawable.ic_settings),
            contentDescription = "Configurar",
            colorFilter = ColorFilter.tint(ColorProvider(look.text)),
            modifier = GlanceModifier
                .size(32.dp)
                .padding(4.dp)
                .then(if (preview) GlanceModifier else GlanceModifier.clickable(actionStartActivity<MainActivity>())),
        )
    }
}

/** Envuelve una fila según el estilo elegido (simple, líneas, rayas o tarjetas). */
@Composable
private fun LineContainer(look: Look, index: Int, content: @Composable () -> Unit) {
    when (look.style.rowStyle) {
        RowStyle.PLAIN -> Box(GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp)) { content() }
        RowStyle.DIVIDERS -> Column(GlanceModifier.fillMaxWidth()) {
            Box(GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp)) { content() }
            Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(look.divider))
        }
        RowStyle.ZEBRA -> Box(
            GlanceModifier
                .fillMaxWidth()
                .then(if (index % 2 == 0) GlanceModifier.background(look.row).cornerRadius(6.dp) else GlanceModifier)
                .padding(horizontal = 4.dp),
        ) { content() }
        RowStyle.CARDS -> Box(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Box(
                GlanceModifier
                    .fillMaxWidth()
                    .background(look.row)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) { content() }
        }
    }
}

@Composable
private fun ExpenseLine(look: Look, row: ExpenseRow, index: Int, interactive: Boolean) {
    val expense = row.expense
    LineContainer(look, index) {
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatMoney(expense.amountCents),
                style = look.text(14f, bold = look.style.boldAmounts),
                modifier = GlanceModifier.width(look.scaled(AmountWidth)),
                maxLines = 1,
            )
            Text(
                expense.description,
                style = look.text(13f),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            // Celda de ingresos: tachado en verde lo sugerido; lo demás dice si alcanza o cuánto falta.
            Box(
                modifier = GlanceModifier
                    .width(look.scaled(IncomeWidth))
                    .padding(horizontal = 2.dp, vertical = 4.dp)
                    .then(if (row.suggested) GlanceModifier.background(look.accentSoft).cornerRadius(6.dp) else GlanceModifier),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    row.suggested -> Text(
                        formatMoney(expense.amountCents),
                        style = look.text(13f, look.accent, bold = true, strike = true),
                        maxLines = 1,
                    )
                    row.canPay -> Text("Alcanza", style = look.text(11f, look.secondary), maxLines = 1)
                    else -> Text("Faltan ${formatMoney(row.shortfall)}", style = look.text(10f, look.expense), maxLines = 1)
                }
            }
            // Visto: pagar con los ingresos. Disponible siempre que los ingresos alcancen.
            Box(modifier = GlanceModifier.width(CheckWidth), contentAlignment = Alignment.CenterEnd) {
                if (row.canPay) {
                    val color = ColorProvider(if (row.suggested) look.accent else look.secondary)
                    CheckBox(
                        checked = false,
                        onCheckedChange = if (interactive) action<PayExpenseAction>(expense.id) else null,
                        colors = CheckboxDefaults.colors(checkedColor = ColorProvider(look.accent), uncheckedColor = color),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToReceiveTitle(look: Look) {
    Text(
        "Por cobrar",
        style = look.text(12f, look.secondary, bold = true),
        modifier = GlanceModifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp),
    )
}

@Composable
private fun ToReceiveLine(look: Look, income: Movement, index: Int, interactive: Boolean) {
    LineContainer(look, index) {
        Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatMoney(income.amountCents),
                style = look.text(14f, look.accent, bold = look.style.boldAmounts),
                modifier = GlanceModifier.width(look.scaled(AmountWidth)),
                maxLines = 1,
            )
            Text(
                income.description.ifBlank { "Ingreso" },
                style = look.text(13f),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            Text(
                formatShortDay(income.expectedEpochDay ?: 0),
                style = look.text(12f, look.secondary),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                "Llegó",
                style = look.text(12f, look.accent, bold = true),
                modifier = GlanceModifier
                    .background(look.accentSoft)
                    .cornerRadius(8.dp)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .then(if (interactive) GlanceModifier.clickable(action<ReceiveIncomeAction>(income.id)) else GlanceModifier),
            )
        }
    }
}

internal inline fun <reified T : ActionCallback> action(id: Long) =
    actionRunCallback<T>(actionParametersOf(IdKey to id))
