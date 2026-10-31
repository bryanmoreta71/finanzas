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
import androidx.glance.layout.RowScope
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
import com.finanzas.app.data.centsToInput
import com.finanzas.app.data.formatMoney
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Colores de sintaxis y de la ventana de cada editor. */
private class Ide(
    val kotlin: Boolean,
    val keyword: Color,
    val variable: Color,
    val string: Color,
    val number: Color,
    val function: Color,
    val comment: Color,
    val lineNumber: Color,
    val caretLine: Color,
    val statusBar: Color,
    val statusText: Color,
    val tabAccent: Color,
) {
    val fileName get() = if (kotlin) "Finanzas.kt" else "finanzas.ts"
    val indent get() = if (kotlin) "    " else "  "
    fun quote(text: String) = if (kotlin) "\"$text\"" else "'$text'"
}

// Tema oscuro de la interfaz nueva de Android Studio.
private val AndroidStudio = Ide(
    kotlin = true,
    keyword = Color(0xFFCF8E6D), variable = Color(0xFFC77DBA), string = Color(0xFF6AAB73),
    number = Color(0xFF2AACB8), function = Color(0xFF56A8F5), comment = Color(0xFF7A7E85),
    lineNumber = Color(0xFF4B5059), caretLine = Color(0xFF26282E),
    statusBar = Color(0xFF2B2D30), statusText = Color(0xFF9DA0A8), tabAccent = Color(0xFF3574F0),
)

// "Dark Modern" de VS Code, con la barra de estado azul de toda la vida.
private val VsCode = Ide(
    kotlin = false,
    keyword = Color(0xFF569CD6), variable = Color(0xFF9CDCFE), string = Color(0xFFCE9178),
    number = Color(0xFFB5CEA8), function = Color(0xFFDCDCAA), comment = Color(0xFF6A9955),
    lineNumber = Color(0xFF6E7681), caretLine = Color(0xFF282828),
    statusBar = Color(0xFF007ACC), statusText = Color(0xFFFFFFFF), tabAccent = Color(0xFF0078D4),
)

// Semáforo de las ventanas de Mac.
private val MacRed = Color(0xFFFF5F57)
private val MacYellow = Color(0xFFFEBC2E)
private val MacGreen = Color(0xFF28C840)

private enum class Kind { TEXT, KEYWORD, VARIABLE, STRING, NUMBER, FUNCTION, COMMENT }

private data class Tok(val text: String, val kind: Kind)

/** Una línea del "código" que se muestra en el editor. */
private sealed interface CodeLine {
    data class Tokens(val tokens: List<Tok>) : CodeLine
    data class Expense(val row: ExpenseRow) : CodeLine
    data class ToReceive(val income: Movement) : CodeLine
    data object Caret : CodeLine
}

private val codeDate = DateTimeFormatter.ofPattern("EEE dd/MM/yyyy", Locale.forLanguageTag("es"))
private val shortDate = DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("es"))

private fun comment(text: String) = CodeLine.Tokens(listOf(Tok(text, Kind.COMMENT)))

/** Arma el archivo: fecha, la lista de egresos y la de ingresos por cobrar. */
private fun buildCode(
    ide: Ide,
    ledger: Ledger,
    today: LocalDate,
    showHeaders: Boolean,
    toReceive: List<Movement>,
    maxExpenses: Int,
): List<CodeLine> = buildList {
    val decl = if (ide.kotlin) "val " else "const "
    fun open(name: String) = CodeLine.Tokens(
        if (ide.kotlin) {
            listOf(Tok(decl, Kind.KEYWORD), Tok(name, Kind.VARIABLE), Tok(" = ", Kind.TEXT), Tok("listOf", Kind.FUNCTION), Tok("(", Kind.TEXT))
        } else {
            listOf(Tok(decl, Kind.KEYWORD), Tok(name, Kind.VARIABLE), Tok(" = [", Kind.TEXT))
        },
    )
    val close = CodeLine.Tokens(listOf(Tok(if (ide.kotlin) ")" else "];", Kind.TEXT)))

    add(comment("// ${today.format(codeDate)}"))
    if (showHeaders) add(comment("// monto, motivo → ingresos, pagar"))
    add(open("egresos"))
    if (ledger.pending.isEmpty()) add(comment("${ide.indent}// sin egresos pendientes"))
    ledger.pending.take(maxExpenses).forEach { add(CodeLine.Expense(it)) }
    add(close)
    if (toReceive.isNotEmpty()) {
        add(open("porCobrar"))
        toReceive.forEach { add(CodeLine.ToReceive(it)) }
        add(close)
    }
}

/**
 * El widget dibujado como Android Studio o VS Code en una Mac: ventana con el semáforo,
 * pestaña del archivo, números de línea, código con colores y barra de estado.
 */
@Composable
internal fun IdeContent(ledger: Ledger, today: LocalDate, look: Look, preview: Boolean) {
    val style = look.style
    val ide = if (style.layout == WidgetLayout.VSCODE) VsCode else AndroidStudio
    val toReceive = if (style.showToReceive) ledger.toReceive else emptyList()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(look.background)
            .cornerRadius(style.cornerRadius.dp),
    ) {
        TitleBar(ide, look, preview)
        TabBar(ide, look)
        Box(modifier = GlanceModifier.defaultWeight().fillMaxWidth().padding(top = 4.dp)) {
            if (preview) {
                val maxExpenses = if (toReceive.isEmpty()) 6 else if (style.showHeaders) 3 else 4
                val lines = buildCode(ide, ledger, today, style.showHeaders, toReceive.take(1), maxExpenses) + CodeLine.Caret
                // Las columnas de Glance admiten hasta 10 hijos: se agrupan las líneas en bloques.
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    lines.withIndex().chunked(8).forEach { chunk ->
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            chunk.forEach { (i, line) -> Line(ide, look, i + 1, line, interactive = false) }
                        }
                    }
                }
            } else {
                val lines = buildCode(ide, ledger, today, style.showHeaders, toReceive, Int.MAX_VALUE) + CodeLine.Caret
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(lines.size, itemId = { it.toLong() }) { i ->
                        Line(ide, look, i + 1, lines[i], interactive = true)
                    }
                }
            }
        }
        StatusBar(ide, look, ledger)
    }
}

@Composable
private fun Dot(color: Color) {
    Image(
        provider = ImageProvider(R.drawable.ic_dot),
        contentDescription = null,
        colorFilter = ColorFilter.tint(ColorProvider(color)),
        modifier = GlanceModifier.size(11.dp),
    )
}

@Composable
private fun TitleBar(ide: Ide, look: Look, preview: Boolean) {
    val openApp = if (preview) GlanceModifier else GlanceModifier.clickable(actionStartActivity<MainActivity>())
    val project = look.style.title.trim().ifEmpty { "finanzas" }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(look.titleBar)
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
            .then(openApp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Las filas de Glance admiten hasta 10 hijos, así que el semáforo va en su propia fila.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(MacRed)
            Spacer(GlanceModifier.width(6.dp))
            Dot(MacYellow)
            Spacer(GlanceModifier.width(6.dp))
            Dot(MacGreen)
        }
        Spacer(GlanceModifier.width(12.dp))
        if (ide.kotlin) {
            // Interfaz nueva: iniciales del proyecto en un cuadrito de color, nombre y rama.
            Box(
                modifier = GlanceModifier.background(Color(0xFF3574F0)).cornerRadius(4.dp).padding(horizontal = 3.dp, vertical = 1.dp),
            ) {
                Text(project.take(2).uppercase(), style = look.text(9f, Color.White, bold = true), maxLines = 1)
            }
            Spacer(GlanceModifier.width(6.dp))
            Text("$project ˅", style = look.text(11f, bold = true), maxLines = 1)
            Text("   ⎇ main ˅", style = look.text(11f, look.secondary), maxLines = 1, modifier = GlanceModifier.defaultWeight())
            Text("app ˅  ", style = look.text(11f, look.secondary), maxLines = 1)
            Text("▶", style = look.text(13f, MacGreen), maxLines = 1)
        } else {
            // Command center centrado con el nombre del proyecto.
            Spacer(GlanceModifier.defaultWeight())
            Box(
                modifier = GlanceModifier
                    .background(look.text.copy(alpha = 0.08f))
                    .cornerRadius(6.dp)
                    .padding(horizontal = 22.dp, vertical = 2.dp),
            ) {
                Text(project, style = look.text(11f, look.secondary), maxLines = 1)
            }
            Spacer(GlanceModifier.defaultWeight())
        }
        Spacer(GlanceModifier.width(10.dp))
        Image(
            provider = ImageProvider(R.drawable.ic_settings),
            contentDescription = "Configurar",
            colorFilter = ColorFilter.tint(ColorProvider(look.secondary)),
            modifier = GlanceModifier.size(16.dp),
        )
    }
}

@Composable
private fun TabBar(ide: Ide, look: Look) {
    // Android Studio pinta la pestaña sobre el fondo del editor con una línea abajo;
    // VS Code tiene la barra más oscura y la pestaña activa con una línea arriba.
    Row(
        modifier = GlanceModifier.fillMaxWidth().background(if (ide.kotlin) look.background else look.titleBar),
    ) {
        Column(modifier = GlanceModifier.background(look.background)) {
            if (!ide.kotlin) Spacer(GlanceModifier.width(TabWidth).height(2.dp).background(ide.tabAccent))
            Row(
                modifier = GlanceModifier.width(TabWidth).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (ide.kotlin) {
                    Text("K", style = look.text(11f, Color(0xFF7F52FF), bold = true), maxLines = 1)
                } else {
                    Box(modifier = GlanceModifier.background(Color(0xFF3178C6)).cornerRadius(2.dp).padding(horizontal = 2.dp)) {
                        Text("TS", style = look.text(7f, Color.White, bold = true), maxLines = 1)
                    }
                }
                Spacer(GlanceModifier.width(6.dp))
                Text(ide.fileName, style = look.text(11f), maxLines = 1, modifier = GlanceModifier.defaultWeight())
                Text("×", style = look.text(12f, look.secondary), maxLines = 1)
            }
            if (ide.kotlin) Spacer(GlanceModifier.width(TabWidth).height(2.dp).background(ide.tabAccent))
        }
    }
    if (ide.kotlin) Spacer(GlanceModifier.fillMaxWidth().height(1.dp).background(look.titleBar))
}

private val TabWidth = 130.dp
private val GutterWidth = 30.dp
private val IncomeWidth = 78.dp

@Composable
private fun Line(ide: Ide, look: Look, number: Int, line: CodeLine, interactive: Boolean) {
    val caret = line is CodeLine.Caret
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .then(if (caret) GlanceModifier.background(ide.caretLine) else GlanceModifier)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            number.toString(),
            style = look.text(11f, if (caret) look.secondary else ide.lineNumber, align = TextAlign.End),
            modifier = GlanceModifier.width(GutterWidth).padding(end = 12.dp),
            maxLines = 1,
        )
        when (line) {
            is CodeLine.Tokens -> line.tokens.forEachIndexed { i, tok ->
                Code(ide, look, tok, last = i == line.tokens.lastIndex)
            }
            is CodeLine.Expense -> ExpenseCode(ide, look, line.row, interactive)
            is CodeLine.ToReceive -> ToReceiveCode(ide, look, line.income, interactive)
            CodeLine.Caret -> Text("▏", style = look.text(12f, look.text), maxLines = 1)
        }
    }
}

@Composable
private fun RowScope.Code(ide: Ide, look: Look, tok: Tok, last: Boolean = false, bold: Boolean = false) {
    val color = when (tok.kind) {
        Kind.TEXT -> look.text
        Kind.KEYWORD -> ide.keyword
        Kind.VARIABLE -> ide.variable
        Kind.STRING -> ide.string
        Kind.NUMBER -> ide.number
        Kind.FUNCTION -> ide.function
        Kind.COMMENT -> ide.comment
    }
    Text(
        tok.text,
        style = look.text(12f, color, bold = bold),
        maxLines = 1,
        modifier = if (last) GlanceModifier.defaultWeight() else GlanceModifier,
    )
}

private fun shorten(text: String, max: Int = 12) = if (text.length <= max) text else text.take(max - 1) + "…"

@Composable
private fun RowScope.ExpenseCode(ide: Ide, look: Look, row: ExpenseRow, interactive: Boolean) {
    val expense = row.expense
    Code(ide, look, Tok("${ide.indent}pagar", Kind.FUNCTION))
    Code(ide, look, Tok("(", Kind.TEXT))
    Code(ide, look, Tok(centsToInput(expense.amountCents), Kind.NUMBER), bold = look.style.boldAmounts)
    Code(ide, look, Tok(", ", Kind.TEXT))
    Code(ide, look, Tok(ide.quote(shorten(expense.description)), Kind.STRING))
    Code(ide, look, Tok("),", Kind.TEXT), last = true)
    Box(modifier = GlanceModifier.width(look.scaled(IncomeWidth))) {
        when {
            row.suggested -> Text(
                formatMoney(expense.amountCents),
                style = look.text(12f, look.accent, bold = true, strike = true),
                maxLines = 1,
            )
            row.canPay -> Text("// alcanza", style = look.text(10f, ide.comment), maxLines = 1)
            else -> Text("// falta ${formatMoney(row.shortfall)}", style = look.text(10f, look.expense), maxLines = 1)
        }
    }
    Box(modifier = GlanceModifier.width(30.dp), contentAlignment = Alignment.Center) {
        if (row.canPay) {
            // Cuadrito del alto de una línea para que el código no quede desparejo.
            Image(
                provider = ImageProvider(R.drawable.ic_checkbox),
                contentDescription = "Pagar con ingresos",
                colorFilter = ColorFilter.tint(ColorProvider(if (row.suggested) look.accent else look.secondary)),
                modifier = GlanceModifier
                    .size(look.scaled(15.dp))
                    .then(if (interactive) GlanceModifier.clickable(action<PayExpenseAction>(expense.id)) else GlanceModifier),
            )
        }
    }
}

@Composable
private fun RowScope.ToReceiveCode(ide: Ide, look: Look, income: Movement, interactive: Boolean) {
    val day = income.expectedEpochDay?.let { LocalDate.ofEpochDay(it).format(shortDate) } ?: ""
    Code(ide, look, Tok("${ide.indent}cobrar", Kind.FUNCTION))
    Code(ide, look, Tok("(", Kind.TEXT))
    Code(ide, look, Tok(centsToInput(income.amountCents), Kind.NUMBER), bold = look.style.boldAmounts)
    Code(ide, look, Tok(", ", Kind.TEXT))
    Code(ide, look, Tok(ide.quote(shorten(income.description.ifBlank { "Ingreso" })), Kind.STRING))
    Code(ide, look, Tok("),", Kind.TEXT), last = true)
    Text("// $day ", style = look.text(10f, ide.comment), maxLines = 1)
    // El "botón de correr" de los editores: marca que el ingreso ya llegó.
    Text(
        "▶ Llegó",
        style = look.text(11f, look.accent, bold = true),
        modifier = GlanceModifier
            .background(look.accent.copy(alpha = 0.15f))
            .cornerRadius(4.dp)
            .padding(horizontal = 5.dp, vertical = 1.dp)
            .then(if (interactive) GlanceModifier.clickable(action<ReceiveIncomeAction>(income.id)) else GlanceModifier),
        maxLines = 1,
    )
    Spacer(GlanceModifier.width(4.dp))
}

@Composable
private fun StatusBar(ide: Ide, look: Look, ledger: Ledger) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ide.statusBar)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (ide.kotlin) "finanzas › ${ide.fileName}" else "⎇ main",
            style = look.text(10f, ide.statusText),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (look.style.showTotals) {
            // En la barra azul de VS Code todo va en blanco; en Android Studio, con color.
            Text("Egresos ", style = look.text(10f, ide.statusText), maxLines = 1)
            Text(
                formatMoney(ledger.pendingExpenses),
                style = look.text(10f, if (ide.kotlin) look.expense else ide.statusText, bold = true),
                maxLines = 1,
            )
            Text("   Ingresos ", style = look.text(10f, ide.statusText), maxLines = 1)
            Text(
                formatMoney(ledger.availableIncome),
                style = look.text(10f, if (ide.kotlin) look.accent else ide.statusText, bold = true),
                maxLines = 1,
            )
        } else {
            Text("UTF-8", style = look.text(10f, ide.statusText), maxLines = 1)
        }
    }
}
