package com.finanzas.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.appwidget.lazy.items
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
import com.finanzas.app.data.PaidWith
import com.finanzas.app.data.formatMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = ColorProvider(Color(0xFF2E7D32))
private val GreenCell = ColorProvider(Color(0x332E7D32))
private val Red = ColorProvider(Color(0xFFC62828))

private val ExpenseIdKey = ActionParameters.Key<Long>("expense_id")
private val PaidWithKey = ActionParameters.Key<String>("paid_with")

private val dateFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es"))

class FinanzasWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val ledger = FinanzasRepository.get(context).currentLedger()
        val today = LocalDate.now().format(dateFormat).replaceFirstChar { it.uppercase() }
        provideContent {
            GlanceTheme {
                WidgetContent(ledger, today)
            }
        }
    }
}

class FinanzasWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FinanzasWidget()
}

/** Se ejecuta al tocar uno de los vistos de una fila. */
class PayExpenseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ExpenseIdKey] ?: return
        val paidWith = parameters[PaidWithKey]?.let(PaidWith::valueOf) ?: return
        FinanzasRepository.get(context).markPaid(id, paidWith)
    }
}

@Composable
private fun WidgetContent(ledger: Ledger, today: String) {
    val onSurface = GlanceTheme.colors.onSurface
    val secondary = GlanceTheme.colors.onSurfaceVariant
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        // Cabecera: fecha de hoy + tuerquita que abre la app.
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = today,
                style = TextStyle(color = onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
                maxLines = 1,
            )
            Image(
                provider = ImageProvider(R.drawable.ic_settings),
                contentDescription = "Configurar",
                colorFilter = ColorFilter.tint(onSurface),
                modifier = GlanceModifier
                    .size(32.dp)
                    .padding(4.dp)
                    .clickable(actionStartActivity<MainActivity>()),
            )
        }

        // Encabezados de columnas.
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val header = TextStyle(color = secondary, fontSize = 11.sp)
            Text("Otra", style = header, modifier = GlanceModifier.width(CheckWidth))
            Text("Monto", style = header, modifier = GlanceModifier.width(AmountWidth))
            Text("Motivo", style = header, modifier = GlanceModifier.defaultWeight())
            Text(
                "Ingresos",
                style = header.copy(textAlign = TextAlign.Center),
                modifier = GlanceModifier.width(IncomeWidth),
            )
            Text(
                "Pagar",
                style = header.copy(textAlign = TextAlign.End),
                modifier = GlanceModifier.width(CheckWidth),
            )
        }

        if (ledger.pending.isEmpty()) {
            Box(
                modifier = GlanceModifier.defaultWeight().fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Sin egresos pendientes",
                    style = TextStyle(color = secondary, fontSize = 13.sp),
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                items(ledger.pending, itemId = { it.expense.id }) { row ->
                    ExpenseRowView(row)
                }
            }
        }

        // Totales.
        Spacer(GlanceModifier.height(4.dp))
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Egresos: ${formatMoney(ledger.pendingExpenses)}",
                style = TextStyle(color = Red, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                "Ingresos: ${formatMoney(ledger.availableIncome)}",
                style = TextStyle(
                    color = Green,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

private val CheckWidth = 40.dp
private val AmountWidth = 64.dp
private val IncomeWidth = 72.dp

@Composable
private fun ExpenseRowView(row: ExpenseRow) {
    val expense = row.expense
    val onSurface = GlanceTheme.colors.onSurface
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Visto izquierdo: pagado con otra fuente.
        Box(modifier = GlanceModifier.width(CheckWidth)) {
            CheckBox(
                checked = false,
                onCheckedChange = payAction(expense.id, PaidWith.OTHER),
            )
        }
        Text(
            formatMoney(expense.amountCents),
            style = TextStyle(color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.width(AmountWidth),
            maxLines = 1,
        )
        Text(
            expense.description,
            style = TextStyle(color = onSurface, fontSize = 13.sp),
            modifier = GlanceModifier.defaultWeight(),
            maxLines = 1,
        )
        // Celda de ingresos: tachado en verde si alcanza.
        Box(
            modifier = GlanceModifier
                .width(IncomeWidth)
                .padding(horizontal = 2.dp, vertical = 4.dp)
                .then(if (row.covered) GlanceModifier.background(GreenCell).cornerRadius(6.dp) else GlanceModifier),
            contentAlignment = Alignment.Center,
        ) {
            if (row.covered) {
                Text(
                    formatMoney(expense.amountCents),
                    style = TextStyle(
                        color = Green,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.LineThrough,
                    ),
                    maxLines = 1,
                )
            } else {
                Text(
                    "Faltan ${formatMoney(row.shortfall)}",
                    style = TextStyle(color = Red, fontSize = 10.sp),
                    maxLines = 1,
                )
            }
        }
        // Visto derecho: pagar con los ingresos (solo si alcanza).
        Box(modifier = GlanceModifier.width(CheckWidth), contentAlignment = Alignment.CenterEnd) {
            if (row.covered) {
                CheckBox(
                    checked = false,
                    onCheckedChange = payAction(expense.id, PaidWith.INCOME),
                    colors = CheckboxDefaults.colors(checkedColor = Green, uncheckedColor = Green),
                )
            }
        }
    }
}

private fun payAction(id: Long, paidWith: PaidWith) = actionRunCallback<PayExpenseAction>(
    actionParametersOf(ExpenseIdKey to id, PaidWithKey to paidWith.name),
)
