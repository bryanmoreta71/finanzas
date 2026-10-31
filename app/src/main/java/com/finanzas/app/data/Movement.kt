package com.finanzas.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MovementType { EXPENSE, INCOME }

/** De dónde salió el dinero para pagar un egreso. */
enum class PaidWith {
    /** Pagado con otra fuente. Ya no se ofrece, pero se conserva para datos viejos. */
    OTHER,

    /** Pagado con los ingresos: se resta de ellos. */
    INCOME,

    /** Gasto del día a día registrado con el botón de gasto rápido: también se resta de los ingresos. */
    SPENT,
}

@Entity(tableName = "movements")
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: MovementType,
    val amountCents: Long,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val paidWith: PaidWith? = null,
    val paidAt: Long? = null,
    /** Solo ingresos: día (epoch day) en que se va a cobrar. Null si ya se recibió. */
    val expectedEpochDay: Long? = null,
) {
    val isPaid: Boolean get() = paidWith != null

    /** Ingreso registrado que todavía no ha llegado ("por cobrar"). */
    val isToReceive: Boolean get() = type == MovementType.INCOME && expectedEpochDay != null
}
