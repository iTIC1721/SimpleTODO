package com.simpletodo.simpletodo.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ScheduleDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ScheduleDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.scheduleDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insert_appearsInCurrentSchedules() = runBlocking {
        dao.insert(Schedule(title = "치과 예약", date = LocalDate.now().plusDays(1), time = LocalTime.of(14, 30)))

        val current = dao.getCurrentSchedules().first()

        assertEquals(1, current.size)
        assertEquals("치과 예약", current[0].title)
    }

    @Test
    fun currentSchedules_sortedByDateThenTime_withNoTimeLast() = runBlocking {
        val today = LocalDate.now()
        dao.insert(Schedule(title = "늦은 일정", date = today, time = LocalTime.of(20, 0)))
        dao.insert(Schedule(title = "시간 미정", date = today, time = null))
        dao.insert(Schedule(title = "이른 일정", date = today, time = LocalTime.of(9, 0)))
        dao.insert(Schedule(title = "내일 일정", date = today.plusDays(1)))

        val current = dao.getCurrentSchedules().first()

        assertEquals(listOf("이른 일정", "늦은 일정", "시간 미정", "내일 일정"), current.map { it.title })
    }

    @Test
    fun markCompleted_movesScheduleFromCurrentToPast() = runBlocking {
        val id = dao.insert(Schedule(title = "완료할 일정", date = LocalDate.now()))

        dao.markCompleted(id, Instant.now())

        assertTrue(dao.getCurrentSchedules().first().isEmpty())
        val past = dao.getPastSchedules().first()
        assertEquals(1, past.size)
        assertEquals("완료할 일정", past[0].title)
        assertTrue(past[0].isCompleted)
    }

    @Test
    fun update_changesScheduleContent() = runBlocking {
        val id = dao.insert(Schedule(title = "원래 제목", date = LocalDate.now()))
        val original = dao.getCurrentSchedules().first().first { it.id == id }

        dao.update(original.copy(title = "수정된 제목", date = LocalDate.now().plusDays(3)))

        val updated = dao.getCurrentSchedules().first().first { it.id == id }
        assertEquals("수정된 제목", updated.title)
        assertEquals(LocalDate.now().plusDays(3), updated.date)
    }

    @Test
    fun markIncomplete_restoresScheduleFromPastToCurrent() = runBlocking {
        val id = dao.insert(Schedule(title = "되돌릴 일정", date = LocalDate.now()))
        dao.markCompleted(id, Instant.now())

        dao.markIncomplete(id)

        assertTrue(dao.getPastSchedules().first().isEmpty())
        val current = dao.getCurrentSchedules().first()
        assertEquals(1, current.size)
        assertEquals("되돌릴 일정", current[0].title)
        assertTrue(!current[0].isCompleted)
        assertEquals(null, current[0].completedAt)
    }

    @Test
    fun pastSchedules_sortedByMostRecentlyCompletedFirst() = runBlocking {
        val id1 = dao.insert(Schedule(title = "먼저 완료", date = LocalDate.now()))
        val id2 = dao.insert(Schedule(title = "나중에 완료", date = LocalDate.now()))
        dao.markCompleted(id1, Instant.now().minusSeconds(60))
        dao.markCompleted(id2, Instant.now())

        val past = dao.getPastSchedules().first()

        assertEquals(listOf("나중에 완료", "먼저 완료"), past.map { it.title })
    }

    @Test
    fun searchPastSchedules_filtersByKeyword() = runBlocking {
        val id1 = dao.insert(Schedule(title = "치과 진료", date = LocalDate.now()))
        val id2 = dao.insert(Schedule(title = "미용실 예약", date = LocalDate.now()))
        dao.markCompleted(id1, Instant.now())
        dao.markCompleted(id2, Instant.now())

        val result = dao.getPastSchedules(keyword = "치과").first()

        assertEquals(1, result.size)
        assertEquals("치과 진료", result[0].title)
    }

    @Test
    fun searchPastSchedules_filtersByDateRange() = runBlocking {
        val id1 = dao.insert(Schedule(title = "지난달 일정", date = LocalDate.now().minusMonths(1)))
        val id2 = dao.insert(Schedule(title = "어제 일정", date = LocalDate.now().minusDays(1)))
        dao.markCompleted(id1, Instant.now())
        dao.markCompleted(id2, Instant.now())

        val result = dao.getPastSchedules(
            startDate = LocalDate.now().minusDays(7),
            endDate = LocalDate.now(),
        ).first()

        assertEquals(listOf("어제 일정"), result.map { it.title })
    }

    @Test
    fun getScheduleById_returnsMatchingScheduleOrNull() = runBlocking {
        val id = dao.insert(Schedule(title = "조회할 일정", date = LocalDate.now()))

        val found = dao.getScheduleById(id)
        val missing = dao.getScheduleById(id + 999)

        assertEquals("조회할 일정", found?.title)
        assertEquals(null, missing)
    }

    @Test
    fun getSchedulesForDate_returnsOnlyThatDatesIncompleteSchedules() = runBlocking {
        val today = LocalDate.now()
        dao.insert(Schedule(title = "오늘 일정", date = today))
        dao.insert(Schedule(title = "내일 일정", date = today.plusDays(1)))
        val completedId = dao.insert(Schedule(title = "오늘 완료된 일정", date = today))
        dao.markCompleted(completedId, Instant.now())

        val result = dao.getSchedulesForDate(today)

        assertEquals(listOf("오늘 일정"), result.map { it.title })
    }

    @Test
    fun deleteListThenInsertAll_restoresTheSameRows() = runBlocking {
        val today = LocalDate.now()
        val keepId = dao.insert(Schedule(title = "남는 일정", date = today))
        val aId = dao.insert(Schedule(title = "지울 일정 1", date = today))
        val bId = dao.insert(Schedule(title = "지울 일정 2", date = today))
        dao.markCompleted(aId, Instant.parse("2026-11-01T09:00:00Z"))
        dao.markCompleted(bId, Instant.parse("2026-11-02T09:00:00Z"))
        val deleted = listOf(dao.getScheduleById(aId)!!, dao.getScheduleById(bId)!!)

        dao.deleteList(deleted)
        assertEquals(listOf(keepId), dao.getAllOnce().map { it.id })
        assertEquals(0, dao.getCompletedCount().first())

        // Undo: the same rows come back with their ids and completion times.
        dao.insertAll(deleted)
        assertEquals(deleted, dao.getPastSchedules().first().sortedBy { it.id })
        assertEquals(2, dao.getCompletedCount().first())
    }
}
