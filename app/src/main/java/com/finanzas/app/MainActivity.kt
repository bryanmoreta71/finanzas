package com.finanzas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzas.app.ui.FinanzasScreen
import com.finanzas.app.ui.FinanzasTheme
import com.finanzas.app.ui.FinanzasViewModel
import com.finanzas.app.ui.WidgetStyleScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanzasTheme {
                val viewModel: FinanzasViewModel = viewModel()
                val ledger by viewModel.ledger.collectAsStateWithLifecycle()
                var customizing by rememberSaveable { mutableStateOf(false) }

                if (customizing) {
                    val style by viewModel.widgetStyle.collectAsStateWithLifecycle()
                    BackHandler { customizing = false }
                    WidgetStyleScreen(
                        ledger = ledger,
                        style = style,
                        onChange = viewModel::updateWidgetStyle,
                        onBack = { customizing = false },
                    )
                } else {
                    FinanzasScreen(ledger, viewModel, onCustomizeWidget = { customizing = true })
                }
            }
        }
    }
}
