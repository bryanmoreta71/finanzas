package com.finanzas.app.widget

import android.content.Context
import org.json.JSONObject

/** Fuentes del sistema que funcionan dentro de un widget. */
enum class WidgetFont(val family: String, val label: String) {
    SANS("sans-serif", "Normal"),
    LIGHT("sans-serif-light", "Ligera"),
    THIN("sans-serif-thin", "Fina"),
    MEDIUM("sans-serif-medium", "Media"),
    BLACK("sans-serif-black", "Gruesa"),
    CONDENSED("sans-serif-condensed", "Condensada"),
    SERIF("serif", "Clásica"),
    MONO("monospace", "Máquina"),
    CASUAL("casual", "Casual"),
    CURSIVE("cursive", "Cursiva"),
    SMALLCAPS("sans-serif-smallcaps", "Versalitas"),
}

/** Cómo se arma el widget: lista normal o consola tipo CMD. */
enum class WidgetLayout(val label: String) {
    NORMAL("Normal"),
    TERMINAL("Terminal (CMD)"),
    ANDROID_STUDIO("Android Studio"),
    VSCODE("VS Code"),
    ;

    /** Diseños que dibujan una ventana de editor de código de Mac. */
    val isIde: Boolean get() = this == ANDROID_STUDIO || this == VSCODE
}

enum class RowStyle(val label: String) {
    PLAIN("Simple"),
    DIVIDERS("Con líneas"),
    ZEBRA("Rayas"),
    CARDS("Tarjetas"),
}

/**
 * Todo lo que se puede personalizar del widget. Los colores son ARGB; la transparencia
 * del fondo y de las filas se guarda aparte para poder moverla con un deslizador.
 */
data class WidgetStyle(
    val background: Int = 0xFFFFFFFF.toInt(),
    val backgroundAlpha: Float = 1f,
    val text: Int = 0xFF1C1B1F.toInt(),
    val accent: Int = 0xFF2E7D32.toInt(),
    val expense: Int = 0xFFC62828.toInt(),
    val row: Int = 0xFFEEEEEE.toInt(),
    val rowAlpha: Float = 1f,
    val font: WidgetFont = WidgetFont.SANS,
    val textScale: Float = 1f,
    val cornerRadius: Int = 16,
    val rowStyle: RowStyle = RowStyle.DIVIDERS,
    val boldAmounts: Boolean = true,
    val title: String = "",
    val showHeaders: Boolean = true,
    val showTotals: Boolean = true,
    val showToReceive: Boolean = true,
    val layout: WidgetLayout = WidgetLayout.NORMAL,
) {
    fun toJson(): String = JSONObject()
        .put("background", background)
        .put("backgroundAlpha", backgroundAlpha.toDouble())
        .put("text", text)
        .put("accent", accent)
        .put("expense", expense)
        .put("row", row)
        .put("rowAlpha", rowAlpha.toDouble())
        .put("font", font.name)
        .put("textScale", textScale.toDouble())
        .put("cornerRadius", cornerRadius)
        .put("rowStyle", rowStyle.name)
        .put("boldAmounts", boldAmounts)
        .put("title", title)
        .put("showHeaders", showHeaders)
        .put("showTotals", showTotals)
        .put("showToReceive", showToReceive)
        .put("layout", layout.name)
        .toString()

    companion object {
        fun fromJson(json: String): WidgetStyle {
            val o = JSONObject(json)
            val d = WidgetStyle()
            return WidgetStyle(
                background = o.optInt("background", d.background),
                backgroundAlpha = o.optDouble("backgroundAlpha", d.backgroundAlpha.toDouble()).toFloat(),
                text = o.optInt("text", d.text),
                accent = o.optInt("accent", d.accent),
                expense = o.optInt("expense", d.expense),
                row = o.optInt("row", d.row),
                rowAlpha = o.optDouble("rowAlpha", d.rowAlpha.toDouble()).toFloat(),
                font = enumOr(o.optString("font"), d.font),
                textScale = o.optDouble("textScale", d.textScale.toDouble()).toFloat(),
                cornerRadius = o.optInt("cornerRadius", d.cornerRadius),
                rowStyle = enumOr(o.optString("rowStyle"), d.rowStyle),
                boldAmounts = o.optBoolean("boldAmounts", d.boldAmounts),
                title = o.optString("title", d.title),
                showHeaders = o.optBoolean("showHeaders", d.showHeaders),
                showTotals = o.optBoolean("showTotals", d.showTotals),
                showToReceive = o.optBoolean("showToReceive", d.showToReceive),
                layout = enumOr(o.optString("layout"), d.layout),
            )
        }

        private inline fun <reified T : Enum<T>> enumOr(name: String, default: T): T =
            enumValues<T>().firstOrNull { it.name == name } ?: default
    }
}

data class WidgetPreset(val name: String, val style: WidgetStyle)

private fun c(hex: Long) = hex.toInt()

val WidgetPresets = listOf(
    WidgetPreset("Clásico", WidgetStyle()),
    // Consola de Windows con la paleta "Campbell": fondo casi negro, texto gris claro.
    WidgetPreset(
        "CMD",
        WidgetStyle(
            layout = WidgetLayout.TERMINAL, background = c(0xFF0C0C0C), text = c(0xFFCCCCCC),
            accent = c(0xFF16C60C), expense = c(0xFFE74856), row = c(0xFF1F1F1F), font = WidgetFont.MONO,
            cornerRadius = 6, boldAmounts = false,
        ),
    ),
    // Terminal "hacker" verde fosforescente con detalles cian y magenta.
    WidgetPreset(
        "Hacker",
        WidgetStyle(
            layout = WidgetLayout.TERMINAL, background = c(0xFF000000), text = c(0xFF00FF41),
            accent = c(0xFF00E5FF), expense = c(0xFFFF0055), row = c(0xFF0A1F0F), font = WidgetFont.MONO,
            cornerRadius = 6, boldAmounts = true,
        ),
    ),
    // Android Studio en Mac con el tema oscuro de la interfaz nueva.
    WidgetPreset(
        "Android Studio",
        WidgetStyle(
            layout = WidgetLayout.ANDROID_STUDIO, background = c(0xFF1E1F22), text = c(0xFFBCBEC4),
            accent = c(0xFF5FB865), expense = c(0xFFF75464), row = c(0xFF2B2D30), font = WidgetFont.MONO,
            cornerRadius = 12, boldAmounts = false,
        ),
    ),
    // VS Code en Mac con el tema "Dark Modern".
    WidgetPreset(
        "VS Code",
        WidgetStyle(
            layout = WidgetLayout.VSCODE, background = c(0xFF1F1F1F), text = c(0xFFCCCCCC),
            accent = c(0xFF89D185), expense = c(0xFFF14C4C), row = c(0xFF181818), font = WidgetFont.MONO,
            cornerRadius = 12, boldAmounts = false,
        ),
    ),
    WidgetPreset(
        "Noche",
        WidgetStyle(
            background = c(0xFF121826), text = c(0xFFF1F5F9), accent = c(0xFF00E676), expense = c(0xFFFF5252),
            row = c(0xFF1E293B), rowStyle = RowStyle.CARDS, font = WidgetFont.MEDIUM, cornerRadius = 24,
        ),
    ),
    WidgetPreset(
        "Transparente",
        WidgetStyle(
            background = c(0xFF000000), backgroundAlpha = 0f, text = c(0xFFFFFFFF), accent = c(0xFF69F0AE),
            expense = c(0xFFFF8A80), row = c(0xFF000000), rowAlpha = 0.4f, rowStyle = RowStyle.CARDS,
            font = WidgetFont.MEDIUM, cornerRadius = 20,
        ),
    ),
    WidgetPreset(
        "Vidrio",
        WidgetStyle(
            background = c(0xFF000000), backgroundAlpha = 0.35f, text = c(0xFFFFFFFF), accent = c(0xFFB9F6CA),
            expense = c(0xFFFFCDD2), row = c(0xFFFFFFFF), rowAlpha = 0.12f, rowStyle = RowStyle.ZEBRA,
            font = WidgetFont.LIGHT, cornerRadius = 28,
        ),
    ),
    WidgetPreset(
        "Neón",
        WidgetStyle(
            background = c(0xFF0D0221), text = c(0xFFF8F8F8), accent = c(0xFF39FF14), expense = c(0xFFFF2079),
            row = c(0xFF261447), rowStyle = RowStyle.ZEBRA, font = WidgetFont.MONO, cornerRadius = 8,
        ),
    ),
    WidgetPreset(
        "Pastel",
        WidgetStyle(
            background = c(0xFFFFF4E6), text = c(0xFF5D4037), accent = c(0xFF43A047), expense = c(0xFFE57373),
            row = c(0xFFFFE0B2), rowStyle = RowStyle.CARDS, font = WidgetFont.CASUAL, cornerRadius = 28,
        ),
    ),
    WidgetPreset(
        "Papel",
        WidgetStyle(
            background = c(0xFFFAF3E0), text = c(0xFF3E2723), accent = c(0xFF33691E), expense = c(0xFFB71C1C),
            row = c(0xFF3E2723), rowAlpha = 0.15f, rowStyle = RowStyle.DIVIDERS, font = WidgetFont.SERIF,
            cornerRadius = 4,
        ),
    ),
    WidgetPreset(
        "Océano",
        WidgetStyle(
            background = c(0xFF0B3D5B), text = c(0xFFE0F7FA), accent = c(0xFF64FFDA), expense = c(0xFFFFAB91),
            row = c(0xFF145374), rowStyle = RowStyle.CARDS, font = WidgetFont.CONDENSED, cornerRadius = 20,
        ),
    ),
    WidgetPreset(
        "Rosa",
        WidgetStyle(
            background = c(0xFFFCE4EC), text = c(0xFF880E4F), accent = c(0xFF00897B), expense = c(0xFFD81B60),
            row = c(0xFFF8BBD0), rowStyle = RowStyle.ZEBRA, font = WidgetFont.CURSIVE, textScale = 1.1f,
            cornerRadius = 32,
        ),
    ),
)

object WidgetStyleStore {
    private const val PREFS = "widget_style"
    private const val KEY = "style"

    fun load(context: Context): WidgetStyle {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return WidgetStyle()
        return runCatching { WidgetStyle.fromJson(json) }.getOrDefault(WidgetStyle())
    }

    fun save(context: Context, style: WidgetStyle) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, style.toJson()).apply()
    }
}
