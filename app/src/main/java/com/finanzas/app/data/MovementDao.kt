package com.finanzas.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementDao {
    @Query("SELECT * FROM movements")
    fun observeAll(): Flow<List<Movement>>

    @Query("SELECT * FROM movements")
    suspend fun getAll(): List<Movement>

    @Query("SELECT * FROM movements WHERE id = :id")
    suspend fun get(id: Long): Movement?

    @Insert
    suspend fun insert(movement: Movement): Long

    @Update
    suspend fun update(movement: Movement)

    @Query("DELETE FROM movements WHERE id = :id")
    suspend fun delete(id: Long)
}
