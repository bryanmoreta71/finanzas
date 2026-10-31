package com.finanzas.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.graphics.Canvas
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.finanzas.app.widget.WidgetPresets
import com.finanzas.app.widget.WidgetStyle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xhdpi")
class WidgetStyleScreenRenderTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun saveButtonSavesTheDraft() {
        var saved = WidgetStyle()
        compose.setContent {
            FinanzasTheme {
                var draft by remember { mutableStateOf(saved) }
                WidgetStyleScreen(
                    ledger = SampleLedger,
                    style = draft,
                    hasChanges = draft != saved,
                    onChange = { draft = it },
                    onSave = { saved = draft },
                    onDiscard = { draft = saved },
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("Sin cambios por guardar").assertExists()
        compose.onNodeWithText("CMD").performClick()
        compose.onNodeWithText("Guardar y aplicar al widget").assertExists()
        snapshot("pantalla-personalizar")
        compose.onNodeWithText("Guardar y aplicar al widget").performClick()
        compose.waitForIdle()
        assertEquals(WidgetPresets.first { it.name == "CMD" }.style, saved)
    }

    @Test
    fun quickExpenseSheetSavesDescriptionAndAmount() {
        var saved: Pair<Long, String>? = null
        compose.setContent {
            FinanzasTheme {
                QuickExpenseSheet(
                    availableCents = 25000,
                    recent = listOf("Encebollado", "Gasolina", "Colas"),
                    onCancel = {},
                    onSave = { cents, text -> saved = cents to text },
                )
            }
        }
        compose.onNodeWithText("Encebollado").performClick()
        compose.onNodeWithText("¿Cuánto?").performTextInput("3.50")
        compose.onNodeWithText("Te quedan $246.50").assertExists()
        snapshot("ventana-gasto")
        compose.onNodeWithText("Guardar gasto").performClick()
        assertEquals(350L to "Encebollado", saved)
    }

    @Test
    fun spendingTabShowsWhereTheMoneyGoes() {
        val now = System.currentTimeMillis()
        val hour = 3_600_000L
        var id = 1L
        fun spent(cents: Long, text: String, ago: Long) = com.finanzas.app.data.Movement(
            id = id++, type = com.finanzas.app.data.MovementType.EXPENSE, amountCents = cents, description = text,
            createdAt = now - ago, paidWith = com.finanzas.app.data.PaidWith.SPENT, paidAt = now - ago,
        )
        val ledger = com.finanzas.app.data.buildLedger(
            listOf(
                com.finanzas.app.data.Movement(id = id++, type = com.finanzas.app.data.MovementType.INCOME, amountCents = 40000, description = "Uber"),
                spent(350, "Encebollado", 1 * hour), spent(1000, "Gasolina", 2 * hour), spent(150, "Cola", 3 * hour),
                spent(350, "Encebollado", 26 * hour), spent(2000, "Gasolina", 27 * hour), spent(500, "Recarga", 28 * hour),
            ),
        )
        compose.setContent { FinanzasTheme { androidx.compose.material3.Surface { SpendingTab(ledger, onUndo = {}) } } }
        compose.onNodeWithText("Todo").performClick()
        compose.onNodeWithText("$43.50").assertExists()
        snapshot("seccion-gastos")
    }

    private fun snapshot(name: String) {
        val dir = System.getProperty("widget.render.dir")?.takeIf { it.isNotBlank() } ?: return
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(dir).mkdirs()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
