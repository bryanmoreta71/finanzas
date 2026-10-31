package com.finanzas.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MovementType { EXPENSE, INCOME }

/** De dónde salió el dinero para pagar un egreso. */
enum class PaidWith {
    /** Visto izquierdo: se pagó con otra fuente, no toca los ingresos. */
    OTHER,

    /** Visto derecho: se pagó con los ingresos, se resta de ellos. */
    INCOME,
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
) {
    val isPaid: Boolean get() = paidWith != null
}
