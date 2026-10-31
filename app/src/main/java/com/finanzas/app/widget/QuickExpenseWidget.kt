package com.finanzas.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import com.finanzas.app.QuickExpenseActivity
import com.finanzas.app.R
import com.finanzas.app.data.FinanzasRepository
import com.finanzas.app.data.formatMoney

/**
 * Botón para registrar un gasto al instante. Muestra cuánto queda de ingresos y usa
 * el mismo diseño que se eligió para el widget principal.
 */
class QuickExpenseWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = FinanzasRepository.get(context)
        val initialLedger = repository.currentLedger()
        val styles = WidgetStyleStore.flow(context)
        provideContent {
            val ledger by repository.ledger.collectAsState(initial = initialLedger)
            val style by styles.collectAsState()
            QuickExpenseContent(ledger.availableIncome, style)
        }
    }
}

class QuickExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickExpenseWidget()
}

@Composable
fun QuickExpenseContent(availableCents: Long, style: WidgetStyle, interactive: Boolean = true) {
    val look = Look(style)
    val compact = LocalSize.current.width < 110.dp
    val money = formatMoney(availableCents)
    val moneyColor = if (availableCents < 0) look.expense else look.accent

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(look.background)
            .cornerRadius(style.cornerRadius.dp)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .then(if (interactive) GlanceModifier.clickable(actionStartActivity<QuickExpenseActivity>()) else GlanceModifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            style.layout == WidgetLayout.TERMINAL -> Column(horizontalAlignment = Alignment.Start) {
                Text(if (compact) ">_+" else "C:\\>gasto_", style = look.text(15f, bold = true), maxLines = 1)
                if (!compact) Text("tienes $money", style = look.text(12f, moneyColor), maxLines = 1)
            }
            style.layout.isIde -> Column(horizontalAlignment = Alignment.Start) {
                Text(if (compact) "▶" else "▶ gasto()", style = look.text(15f, look.accent, bold = true), maxLines = 1)
                if (!compact) Text("// tienes $money", style = look.text(12f, look.secondary), maxLines = 1)
            }
            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                PlusCircle(look)
                if (!compact) {
                    Spacer(GlanceModifier.width(10.dp))
                    Column {
                        Text("Gasto", style = look.text(16f, bold = true), maxLines = 1)
                        Text("Tienes $money", style = look.text(12f, moneyColor, bold = true), maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Círculo del color de los ingresos con un "+"; el círculo es una imagen para que sea redondo en cualquier Android. */
@Composable
private fun PlusCircle(look: Look) {
    val onAccent = if (look.accent.luminance() > 0.5f) Color.Black else Color.White
    Box(contentAlignment = Alignment.Center) {
        Image(
            provider = ImageProvider(R.drawable.ic_dot),
            contentDescription = null,
            colorFilter = ColorFilter.tint(ColorProvider(look.accent)),
            modifier = GlanceModifier.size(38.dp),
        )
        Text("+", style = look.text(22f, onAccent, bold = true), maxLines = 1)
    }
}
