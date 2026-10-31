package com.finanzas.app.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import com.finanzas.app.ui.SampleLedger
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Dibuja el widget con cada estilo a PNG para revisarlo a ojo. Solo corre si se pide:
 * ./gradlew testDebugUnitTest -Pwidget.render.dir=/ruta/de/salida
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-xhdpi")
class WidgetRenderTest {
    /** El widget de verdad (con lista desplazable y acciones) se arma sin errores en todos los estilos. */
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    @Test
    fun realWidgetComposesInEveryStyle() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (preset in WidgetPresets) {
            for (style in listOf(preset.style, preset.style.copy(showHeaders = false, showToReceive = false))) {
                val views = GlanceRemoteViews().compose(context, DpSize(340.dp, 300.dp)) {
                    WidgetContent(SampleLedger, java.time.LocalDate.of(2026, 10, 31), style, preview = false)
                }.remoteViews
                views.apply(context, FrameLayout(context))
            }
        }
    }

    /** El botón de gasto rápido en varios estilos y tamaños, lado a lado en una sola imagen. */
    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    @Test
    fun renderQuickExpenseButton() = runBlocking {
        val outDir = System.getProperty("widget.render.dir")?.takeIf { it.isNotBlank() }
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val density = context.resources.displayMetrics.density
        val names = listOf("Clásico", "Noche", "CMD", "Hacker", "Android Studio", "VS Code")
        val sizes = listOf(170 to 70, 70 to 70)
        val cellW = 190
        val cellH = 90
        val bitmap = Bitmap.createBitmap((cellW * sizes.size * density).toInt(), (cellH * names.size * density).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), 0xFF6A11CB.toInt(), 0xFF2575FC.toInt(), Shader.TileMode.CLAMP)
        })
        names.forEachIndexed { row, name ->
            val style = WidgetPresets.first { it.name == name }.style
            sizes.forEachIndexed { col, (wDp, hDp) ->
                val views = GlanceRemoteViews().compose(context, DpSize(wDp.dp, hDp.dp)) {
                    QuickExpenseContent(23050, style, interactive = false)
                }.remoteViews
                val view = views.apply(context, FrameLayout(context))
                val w = (wDp * density).toInt()
                val h = (hDp * density).toInt()
                view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, w, h)
                canvas.save()
                canvas.translate((col * cellW + 10) * density, (row * cellH + 10) * density)
                view.draw(canvas)
                canvas.restore()
            }
        }
        if (outDir != null) {
            File(outDir).mkdirs()
            File(outDir, "boton-gasto.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    @Test
    fun renderPresets() = runBlocking {
        val outDir = System.getProperty("widget.render.dir")?.takeIf { it.isNotBlank() }
        assumeTrue(outDir != null)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = File(outDir!!).apply { mkdirs() }
        val density = context.resources.displayMetrics.density
        val (wDp, hDp) = 340 to 300

        WidgetPresets.forEach { preset ->
            val views = GlanceRemoteViews().compose(context, DpSize(wDp.dp, hDp.dp)) {
                WidgetContent(SampleLedger, java.time.LocalDate.of(2026, 10, 31), preset.style, preview = true)
            }.remoteViews
            val frame = FrameLayout(context)
            val view = views.apply(context, frame)
            val w = (wDp * density).toInt()
            val h = (hDp * density).toInt()
            view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, w, h)

            val pad = (16 * density).toInt()
            val bitmap = Bitmap.createBitmap(w + 2 * pad, h + 2 * pad, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), Paint().apply {
                shader = LinearGradient(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), 0xFF6A11CB.toInt(), 0xFF2575FC.toInt(), Shader.TileMode.CLAMP)
            })
            canvas.translate(pad.toFloat(), pad.toFloat())
            view.draw(canvas)
            File(dir, "widget-${preset.name}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
