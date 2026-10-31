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
