package com.finanzas.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanzas.app.data.formatMoney
import com.finanzas.app.data.parseMoney

/**
 * Tarjeta para anotar un gasto rápido: primero en qué se gastó, luego cuánto. Se abre
 * con el teclado listo y el gasto se resta de los ingresos al guardar.
 */
@Composable
fun QuickExpenseSheet(
    availableCents: Long,
    recent: List<String>,
    onCancel: () -> Unit,
    onSave: (cents: Long, description: String) -> Unit,
) {
    var description by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    val descriptionFocus = remember { FocusRequester() }
    val amountFocus = remember { FocusRequester() }
    val cents = parseMoney(amount)
    val canSave = description.isNotBlank() && cents != null
    val save = { if (canSave) onSave(cents!!, description) }

    LaunchedEffect(Unit) { descriptionFocus.requestFocus() }

    // Fondo oscurecido: tocar fuera cierra sin guardar.
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                // Que tocar la tarjeta no la cierre.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Column(
                Modifier.navigationBarsPadding().padding(top = 20.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Nuevo gasto", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Text(
                        "Tienes ${formatMoney(availableCents)}",
                        color = if (availableCents < 0) ExpenseRed else IncomeGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("¿En qué gastaste?") },
                    placeholder = { Text("Ej. Encebollado") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { amountFocus.requestFocus() }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).focusRequester(descriptionFocus),
                )
                if (recent.isNotEmpty()) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recent) { text ->
                            SuggestionChip(
                                onClick = {
                                    description = text
                                    amountFocus.requestFocus()
                                },
                                label = { Text(text) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("¿Cuánto?") },
                    prefix = { Text("$") },
                    singleLine = true,
                    isError = amount.isNotBlank() && cents == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).focusRequester(amountFocus),
                )
                if (cents != null) {
                    val left = availableCents - cents
                    Text(
                        if (left >= 0) "Te quedan ${formatMoney(left)}" else "Te pasas por ${formatMoney(-left)} de lo que tienes",
                        color = if (left >= 0) IncomeGreen else ExpenseRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCancel) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = save, enabled = canSave) { Text("Guardar gasto") }
                }
            }
        }
    }
}
