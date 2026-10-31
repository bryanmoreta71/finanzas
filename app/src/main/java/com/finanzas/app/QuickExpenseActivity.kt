package com.finanzas.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.finanzas.app.data.FinanzasRepository
import com.finanzas.app.data.Ledger
import com.finanzas.app.data.formatMoney
import com.finanzas.app.ui.FinanzasTheme
import com.finanzas.app.ui.QuickExpenseSheet
import kotlinx.coroutines.launch

/** Ventanita que abre el widget de gasto rápido: motivo, valor y listo. */
class QuickExpenseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = FinanzasRepository.get(this)
        setContent {
            FinanzasTheme {
                val ledger by repository.ledger.collectAsStateWithLifecycle(initialValue = Ledger.EMPTY)
                QuickExpenseSheet(
                    availableCents = ledger.availableIncome,
                    recent = ledger.recentDescriptions,
                    onCancel = ::finish,
                    onSave = { cents, description ->
                        lifecycleScope.launch {
                            repository.addSpending(cents, description)
                            val left = formatMoney(ledger.availableIncome - cents)
                            Toast.makeText(
                                this@QuickExpenseActivity,
                                "Gasto anotado: ${formatMoney(cents)} en ${description.trim()}. Te quedan $left",
                                Toast.LENGTH_LONG,
                            ).show()
                            finish()
                        }
                    },
                )
            }
        }
    }
}
