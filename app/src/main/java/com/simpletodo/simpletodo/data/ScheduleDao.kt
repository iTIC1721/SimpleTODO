package com.simpletodo.simpletodo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface ScheduleDao {

    @Query(
        """
        SELECT * FROM schedule
        WHERE isCompleted = 0
        ORDER BY date ASC, time IS NULL ASC, time ASC, isImportant DESC, createdAt ASC
        """
    )
    fun getCurrentSchedules(): Flow<List<Schedule>>

    @Query(
        """
        SELECT * FROM schedule
        WHERE isCompleted = 1
        AND (:keyword IS NULL OR title LIKE '%' || :keyword || '%')
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY completedAt DESC
        """
    )
    fun getPastSchedules(
        keyword: String? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): Flow<List<Schedule>>

    @Query("SELECT COUNT(*) FROM schedule WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>

    @Insert
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule)

    @Delete
    suspend fun delete(schedule: Schedule)

    @Query("UPDATE schedule SET isCompleted = 1, completedAt = :completedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, completedAt: Instant = Instant.now())

    @Query("UPDATE schedule SET isCompleted = 0, completedAt = NULL WHERE id = :id")
    suspend fun markIncomplete(id: Long)

    @Query("SELECT * FROM schedule WHERE id = :id")
    suspend fun getScheduleById(id: Long): Schedule?

    @Query(
        """
        SELECT * FROM schedule
        WHERE isCompleted = 0 AND date = :date
        ORDER BY time IS NULL ASC, time ASC, isImportant DESC, createdAt ASC
        """
    )
    suspend fun getSchedulesForDate(date: LocalDate): List<Schedule>

    @Query("SELECT * FROM schedule ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<Schedule>

    @Insert
    suspend fun insertAll(schedules: List<Schedule>)

    @Query("DELETE FROM schedule")
    suspend fun deleteAll()
}
