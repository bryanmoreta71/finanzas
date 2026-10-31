package com.finanzas.app.data

import com.finanzas.app.widget.RowStyle
import com.finanzas.app.widget.WidgetFont
import com.finanzas.app.widget.WidgetPresets
import com.finanzas.app.widget.WidgetStyle
import com.finanzas.app.widget.WidgetStyleStore
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetStyleTest {
    @Test
    fun styleSurvivesJsonRoundTrip() {
        WidgetPresets.forEach { preset ->
            val style = preset.style.copy(title = "Mis pagos 💸")
            assertEquals(style, WidgetStyle.fromJson(style.toJson()))
        }
        val custom = WidgetStyle(font = WidgetFont.CURSIVE, rowStyle = RowStyle.ZEBRA, backgroundAlpha = 0.25f, textScale = 1.3f)
        assertEquals(custom, WidgetStyle.fromJson(custom.toJson()))
    }

    @Test
    fun savingPersistsAndNotifiesTheWidget() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val flow = WidgetStyleStore.flow(context)
        val cmd = WidgetPresets.first { it.name == "CMD" }.style
        WidgetStyleStore.save(context, cmd)
        assertEquals(cmd, flow.value)
        val json = context.getSharedPreferences("widget_style", android.content.Context.MODE_PRIVATE).getString("style", null)
        assertEquals(cmd, WidgetStyle.fromJson(json!!))
    }

    @Test
    fun missingFieldsFallBackToDefaults() {
        assertEquals(WidgetStyle(), WidgetStyle.fromJson("{}"))
    }
}
