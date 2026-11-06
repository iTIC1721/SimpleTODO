package com.simpletodo.simpletodo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringReminderDao {

    @Query("SELECT * FROM recurring_reminder ORDER BY createdAt ASC")
    fun getAll(): Flow<List<RecurringReminder>>

    @Query("SELECT * FROM recurring_reminder WHERE isEnabled = 1")
    suspend fun getAllEnabled(): List<RecurringReminder>

    @Query("SELECT * FROM recurring_reminder WHERE id = :id")
    suspend fun getById(id: Long): RecurringReminder?

    @Insert
    suspend fun insert(reminder: RecurringReminder): Long

    @Update
    suspend fun update(reminder: RecurringReminder)

    @Delete
    suspend fun delete(reminder: RecurringReminder)

    @Query("SELECT * FROM recurring_reminder ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<RecurringReminder>

    @Insert
    suspend fun insertAll(reminders: List<RecurringReminder>)

    @Query("DELETE FROM recurring_reminder")
    suspend fun deleteAll()
}
