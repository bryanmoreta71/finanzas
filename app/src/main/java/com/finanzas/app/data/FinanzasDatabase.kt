package com.finanzas.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromType(type: MovementType): String = type.name

    @TypeConverter
    fun toType(value: String): MovementType = MovementType.valueOf(value)

    @TypeConverter
    fun fromPaidWith(paidWith: PaidWith?): String? = paidWith?.name

    @TypeConverter
    fun toPaidWith(value: String?): PaidWith? = value?.let(PaidWith::valueOf)
}

@Database(entities = [Movement::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class FinanzasDatabase : RoomDatabase() {
    abstract fun movementDao(): MovementDao

    companion object {
        @Volatile
        private var instance: FinanzasDatabase? = null

        fun get(context: Context): FinanzasDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanzasDatabase::class.java,
                    "finanzas.db",
                ).build().also { instance = it }
            }
    }
}
