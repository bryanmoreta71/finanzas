package com.finanzas.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzas.app.data.Movement
import com.finanzas.app.data.MovementType
import com.finanzas.app.data.centsToInput
import com.finanzas.app.data.formatLongDay
import com.finanzas.app.data.parseMoney
import com.finanzas.app.data.relativeDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private const val DAY_MILLIS = 86_400_000L

/**
 * Diálogo para añadir o editar un egreso o un ingreso. Los ingresos pueden quedar
 * "por cobrar" con la fecha en que van a llegar.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovementDialog(
    initialType: MovementType,
    initial: Movement?,
    onDismiss: () -> Unit,
    onSave: (type: MovementType, cents: Long, description: String, expectedEpochDay: Long?) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val today = LocalDate.now()
    val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).toEpochDay()

    var type by remember { mutableStateOf(initialType) }
    var amount by remember { mutableStateOf(initial?.let { centsToInput(it.amountCents) } ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var toReceive by remember { mutableStateOf(initial?.isToReceive ?: false) }
    var expectedDay by remember { mutableStateOf(initial?.expectedEpochDay ?: nextMonday) }
    var pickingDate by remember { mutableStateOf(false) }

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
                        FilterChip(selected = isExpense, onClick = { type = MovementType.EXPENSE }, label = { Text("Egreso") })
                        FilterChip(selected = !isExpense, onClick = { type = MovementType.INCOME }, label = { Text("Ingreso") })
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
                    label = { Text(if (isExpense) "Motivo o persona" else "De dónde (ej. Uber)") },
                    singleLine = true,
                )
                if (!isExpense) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Por cobrar")
                            Text(
                                "Todavía no me llega",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = toReceive, onCheckedChange = { toReceive = it })
                    }
                    if (toReceive) {
                        Text("¿Cuándo te pagan?", style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = expectedDay == nextMonday,
                                onClick = { expectedDay = nextMonday },
                                label = { Text("El lunes") },
                            )
                            FilterChip(
                                selected = expectedDay == today.toEpochDay() + 1,
                                onClick = { expectedDay = today.toEpochDay() + 1 },
                                label = { Text("Mañana") },
                            )
                            FilterChip(
                                selected = false,
                                onClick = { pickingDate = true },
                                label = { Text("Otra fecha…") },
                            )
                        }
                        Text(
                            "${formatLongDay(expectedDay)} (${relativeDay(expectedDay)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = cents != null && (!isExpense || description.isNotBlank()),
                onClick = {
                    cents?.let { onSave(type, it, description, expectedDay.takeIf { !isExpense && toReceive }) }
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = expectedDay * DAY_MILLIS)
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { expectedDay = Math.floorDiv(it, DAY_MILLIS) }
                    pickingDate = false
                }) { Text("Listo") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state)
        }
    }
}
