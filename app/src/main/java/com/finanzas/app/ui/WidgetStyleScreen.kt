package com.finanzas.app.ui

import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.finanzas.app.data.Ledger
import com.finanzas.app.data.Movement
import com.finanzas.app.data.MovementType
import com.finanzas.app.data.buildLedger
import com.finanzas.app.widget.RowStyle
import com.finanzas.app.widget.WidgetContent
import com.finanzas.app.widget.WidgetFont
import com.finanzas.app.widget.WidgetPresets
import com.finanzas.app.widget.WidgetStyle
import com.finanzas.app.widget.WidgetLayout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Fondos de pantalla de mentira para ver cómo queda el widget, sobre todo si es transparente. */
private val Wallpapers = listOf(
    Brush.linearGradient(listOf(Color(0xFF6A11CB), Color(0xFF2575FC))),
    Brush.linearGradient(listOf(Color(0xFFFF9A8B), Color(0xFFFF6A88), Color(0xFFFF99AC))),
    Brush.linearGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
    Brush.linearGradient(listOf(Color(0xFFE0EAFC), Color(0xFFCFDEF3))),
)

private val Swatches = listOf(
    0xFFFFFFFF, 0xFFF5F5F5, 0xFF9E9E9E, 0xFF424242, 0xFF1C1B1F, 0xFF000000,
    0xFFFFF4E6, 0xFFFAF3E0, 0xFFFFE0B2, 0xFFFCE4EC, 0xFFF8BBD0, 0xFFFF80AB,
    0xFFFF5252, 0xFFC62828, 0xFFFF2079, 0xFFE91E63, 0xFF9C27B0, 0xFF7C4DFF,
    0xFF3F51B5, 0xFF2196F3, 0xFF0B3D5B, 0xFF121826, 0xFF0D0221, 0xFF00BCD4,
    0xFF64FFDA, 0xFF009688, 0xFF2E7D32, 0xFF4CAF50, 0xFF00E676, 0xFF39FF14,
    0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722, 0xFF795548,
).map { it.toInt() }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WidgetStyleScreen(
    ledger: Ledger,
    style: WidgetStyle,
    onChange: (WidgetStyle) -> Unit,
    onBack: () -> Unit,
) {
    var wallpaper by remember { mutableStateOf(0) }
    val previewLedger = if (ledger.pending.isEmpty() && ledger.toReceive.isEmpty()) SampleLedger else ledger

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalizar widget") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // Vista previa fija arriba, sobre un "fondo de pantalla".
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Wallpapers[wallpaper])
                    .padding(16.dp),
            ) {
                WidgetPreview(previewLedger, style, Modifier.fillMaxSize())
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (previewLedger === SampleLedger) "Vista previa con datos de ejemplo" else "Fondo de prueba",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Wallpapers.forEachIndexed { i, brush ->
                            Box(
                                Modifier
                                    .padding(start = 6.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(brush)
                                    .border(2.dp, if (i == wallpaper) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                    .clickable { wallpaper = i },
                            )
                        }
                    }
                }

                item { Section("Estilos rápidos") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(WidgetPresets) { preset ->
                            PresetCard(preset.name, preset.style) {
                                onChange(preset.style.copy(title = style.title))
                            }
                        }
                    }
                }

                item { Section("Título") }
                item {
                    OutlinedTextField(
                        value = style.title,
                        onValueChange = { onChange(style.copy(title = it)) },
                        placeholder = {
                            Text(
                                when {
                                    style.layout == WidgetLayout.TERMINAL -> "Título de la ventana (vacío = cmd.exe)"
                                    style.layout.isIde -> "Nombre del proyecto (vacío = finanzas)"
                                    else -> "Ej. Mis pagos 💸 (vacío = solo la fecha)"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    )
                }

                item { Section("Tipo de letra") }
                item {
                    FlowRow(
                        Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WidgetFont.entries.forEach { font ->
                            FilterChip(
                                selected = style.font == font,
                                onClick = { onChange(style.copy(font = font)) },
                                label = { Text(font.label, fontFamily = font.composeFamily(), fontSize = 15.sp) },
                            )
                        }
                    }
                }
                item {
                    LabeledSlider("Tamaño de letra", "${(style.textScale * 100).toInt()}%", style.textScale, 0.8f..1.4f, steps = 11) {
                        onChange(style.copy(textScale = it))
                    }
                }
                item {
                    SwitchRow("Montos en negrita", style.boldAmounts) { onChange(style.copy(boldAmounts = it)) }
                }

                item { Section("Colores") }
                item { ColorRow("Fondo", style.background) { onChange(style.copy(background = it)) } }
                item {
                    LabeledSlider("Transparencia del fondo", "${((1 - style.backgroundAlpha) * 100).toInt()}%", 1 - style.backgroundAlpha, 0f..1f) {
                        onChange(style.copy(backgroundAlpha = 1 - it))
                    }
                }
                item { ColorRow("Letras", style.text) { onChange(style.copy(text = it)) } }
                item { ColorRow("Ingresos y tachado", style.accent) { onChange(style.copy(accent = it)) } }
                item { ColorRow("Egresos y faltantes", style.expense) { onChange(style.copy(expense = it)) } }
                item { ColorRow(if (style.layout == WidgetLayout.NORMAL) "Filas" else "Barra de título", style.row) { onChange(style.copy(row = it)) } }
                item {
                    LabeledSlider("Transparencia de las filas", "${((1 - style.rowAlpha) * 100).toInt()}%", 1 - style.rowAlpha, 0f..1f) {
                        onChange(style.copy(rowAlpha = 1 - it))
                    }
                }

                item { Section("Forma") }
                item {
                    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetLayout.entries.forEach { layout ->
                            FilterChip(
                                selected = style.layout == layout,
                                onClick = { onChange(style.copy(layout = layout)) },
                                label = { Text(layout.label) },
                            )
                        }
                    }
                }
                item {
                    FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RowStyle.entries.forEach { rowStyle ->
                            FilterChip(
                                selected = style.rowStyle == rowStyle,
                                onClick = { onChange(style.copy(rowStyle = rowStyle)) },
                                label = { Text(rowStyle.label) },
                            )
                        }
                    }
                }
                item {
                    LabeledSlider("Esquinas redondeadas", "${style.cornerRadius} dp", style.cornerRadius.toFloat(), 0f..32f, steps = 7) {
                        onChange(style.copy(cornerRadius = it.toInt()))
                    }
                }

                item { Section("Mostrar") }
                item { SwitchRow("Títulos de columnas", style.showHeaders) { onChange(style.copy(showHeaders = it)) } }
                item { SwitchRow("Totales abajo", style.showTotals) { onChange(style.copy(showTotals = it)) } }
                item { SwitchRow("Ingresos por cobrar", style.showToReceive) { onChange(style.copy(showToReceive = it)) } }

                item {
                    OutlinedButton(
                        onClick = { onChange(WidgetStyle()) },
                        modifier = Modifier.padding(16.dp),
                    ) { Text("Restablecer") }
                }
                item {
                    Text(
                        "Los cambios se aplican solos al widget de tu pantalla de inicio. " +
                            "Las esquinas redondeadas se ven en Android 12 o más nuevo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}

/** Dibuja el widget de verdad (mismo código que en la pantalla de inicio) dentro de la app. */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@Composable
fun WidgetPreview(ledger: Ledger, style: WidgetStyle, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    BoxWithConstraints(modifier) {
        val size = DpSize(maxWidth, maxHeight)
        var remoteViews by remember { mutableStateOf<RemoteViews?>(null) }
        LaunchedEffect(ledger, style, size) {
            remoteViews = GlanceRemoteViews()
                .compose(context, size) { WidgetContent(ledger, LocalDate.now(), style, preview = true) }
                .remoteViews
        }
        AndroidView(
            factory = { FrameLayout(it) },
            update = { frame ->
                val views = remoteViews ?: return@AndroidView
                frame.removeAllViews()
                frame.addView(
                    views.apply(frame.context, frame),
                    FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Datos de ejemplo para la vista previa cuando todavía no hay nada registrado. */
internal val SampleLedger: Ledger by lazy {
    val monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).toEpochDay()
    var id = 1L
    fun e(cents: Long, text: String) = Movement(id = id++, type = MovementType.EXPENSE, amountCents = cents, description = text, createdAt = id)
    buildLedger(
        listOf(
            Movement(id = id++, type = MovementType.INCOME, amountCents = 25000, description = "Uber"),
            e(2000, "Netflix"), e(3500, "Juan"), e(6000, "Agua"), e(20000, "Renta"), e(30000, "Tarjeta"),
            Movement(id = id++, type = MovementType.INCOME, amountCents = 40000, description = "Uber", expectedEpochDay = monday),
        ),
    )
}

private fun WidgetFont.composeFamily(): FontFamily = FontFamily(Typeface.create(family, Typeface.NORMAL))

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun PresetCard(name: String, style: WidgetStyle, onClick: () -> Unit) {
    val family = style.font.composeFamily()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(
            Modifier
                .size(width = 84.dp, height = 60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Wallpapers[0])
                .background(Color(style.background).copy(alpha = style.backgroundAlpha))
                .padding(8.dp),
        ) {
            Column {
                if (style.layout == WidgetLayout.TERMINAL) {
                    Text("C:\\>_", color = Color(style.text), fontFamily = family, fontSize = 13.sp)
                } else if (style.layout.isIde) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf(0xFFFF5F57, 0xFFFEBC2E, 0xFF28C840).forEach {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(it)))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("</>", color = Color(style.text), fontFamily = family, fontSize = 13.sp)
                } else {
                    Text("$50", color = Color(style.text), fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(
                    "$20",
                    color = Color(style.accent),
                    fontFamily = family,
                    fontSize = 13.sp,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(style.expense)),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(name, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row {
            Text(label, modifier = Modifier.weight(1f))
            Text(valueText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range, steps = steps)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ColorRow(label: String, color: Int, onChange: (Int) -> Unit) {
    var custom by remember { mutableStateOf(false) }
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                // Selector libre: cualquier color.
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                        .clickable { custom = true },
                    contentAlignment = Alignment.Center,
                ) { Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
            items(Swatches) { swatch ->
                val selected = swatch == color
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(swatch))
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        )
                        .clickable { onChange(swatch) },
                )
            }
        }
    }
    if (custom) {
        ColorPickerDialog(initial = color, onDismiss = { custom = false }, onPick = { onChange(it); custom = false })
    }
}

/** Elige cualquier color con tono, intensidad y brillo. */
@Composable
private fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val start = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var hue by remember { mutableStateOf(start[0]) }
    var saturation by remember { mutableStateOf(start[1]) }
    var value by remember { mutableStateOf(start[2]) }
    val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Elige un color") },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(color)),
                )
                Spacer(Modifier.height(12.dp))
                Text("Tono")
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Text("Intensidad")
                Slider(value = saturation, onValueChange = { saturation = it })
                Text("Brillo")
                Slider(value = value, onValueChange = { value = it })
                Text(
                    "#%06X".format(color and 0xFFFFFF),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onPick(Color(color).toArgb()) }) { Text("Usar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
