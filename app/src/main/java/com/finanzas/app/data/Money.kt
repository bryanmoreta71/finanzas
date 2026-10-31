package com.finanzas.app.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val symbols = DecimalFormatSymbols(Locale.US)
private val wholeFormat = DecimalFormat("#,##0", symbols)
private val centsFormat = DecimalFormat("#,##0.00", symbols)

/** 5000 -> "$50", 5050 -> "$50.50", -300 -> "-$3". */
fun formatMoney(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val number = if (abs % 100 == 0L) {
        wholeFormat.format(abs / 100)
    } else {
        centsFormat.format(BigDecimal.valueOf(abs, 2))
    }
    return "$sign$$number"
}

/**
 * Convierte lo que escribe el usuario a centavos. Acepta "50", "50.5", "50,5",
 * "1,250.75" y "$ 20". Devuelve null si no es un monto positivo válido.
 */
fun parseMoney(input: String): Long? {
    var text = input.trim().removePrefix("$").replace(" ", "")
    if (text.isEmpty()) return null
    text = if (text.contains('.') && text.contains(',')) {
        text.replace(",", "")
    } else {
        text.replace(',', '.')
    }
    val value = text.toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0) return null
    return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
}

fun centsToInput(cents: Long): String =
    BigDecimal.valueOf(cents, 2).stripTrailingZeros().toPlainString()

private val spanish = Locale.forLanguageTag("es")
private val shortDay = java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", spanish)
private val longDay = java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", spanish)

/** 20395 -> "lun 3 nov". */
fun formatShortDay(epochDay: Long): String =
    java.time.LocalDate.ofEpochDay(epochDay).format(shortDay).replace(".", "")

/** 20395 -> "Lunes 3 de noviembre". */
fun formatLongDay(epochDay: Long): String =
    java.time.LocalDate.ofEpochDay(epochDay).format(longDay).replaceFirstChar { it.uppercase() }

/** "hoy", "mañana", "en 3 días", "hace 2 días". */
fun relativeDay(epochDay: Long, today: Long = java.time.LocalDate.now().toEpochDay()): String =
    when (val diff = epochDay - today) {
        0L -> "hoy"
        1L -> "mañana"
        -1L -> "ayer"
        in 2L..Long.MAX_VALUE -> "en $diff días"
        else -> "hace ${-diff} días"
    }
