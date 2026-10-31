package com.finanzas.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE movements ADD COLUMN expectedEpochDay INTEGER")
    }
}

@Database(entities = [Movement::class], version = 2, exportSchema = false)
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
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
