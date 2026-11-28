package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CachedBookmarkEntity
import com.example.data.local.CachedDownloadEntity
import com.example.data.local.CachedQuizAttemptEntity
import com.example.data.local.CachedReadingProgressEntity
import com.example.data.local.StudyHubDatabase
import com.example.data.model.QuizAttemptRecord
import com.example.data.model.ReadingProgressRecord
import com.example.data.model.SupabaseMappers
import com.example.data.model.UserProfile
import com.example.data.model.UserStatsCalculator
import com.example.data.remote.SeedCurriculumData
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DataAccuracyTest {

    @Test
    fun `user profile and seed classes default to zero instead of fake metrics`() {
        val freshProfile = UserProfile(id = "u1", userId = "u1")
        assertEquals(0L, freshProfile.avgScore)
        assertEquals(0L, freshProfile.dayStreak)
        assertEquals(0L, freshProfile.points)
        assertEquals(0L, freshProfile.dailyCompletedCount)

        val mappedEmpty = SupabaseMappers.userProfileFromJson(JSONObject().put("id", "u1"))
        assertEquals(0L, mappedEmpty.avgScore)
        assertEquals(0L, mappedEmpty.dayStreak)
        assertEquals(0L, mappedEmpty.points)
        assertEquals(0L, mappedEmpty.dailyCompletedCount)

        assertTrue(SeedCurriculumData.classes.all { it.studentCount == 0L })
    }

    @Test
    fun `user stats calculator computes zero when user has no quiz attempts or reads`() {
        val stats = UserStatsCalculator.compute(
            quizAttempts = emptyList(),
            readingProgress = emptyList()
        )
        assertEquals(0L, stats.avgScore)
        assertEquals(0L, stats.dayStreak)
        assertEquals(0L, stats.points)
        assertEquals(0L, stats.dailyCompletedCount)
    }

    @Test
    fun `user stats calculator computes exact average score, points, daily count, and consecutive streak`() {
        val zone = ZoneOffset.UTC
        val today = LocalDate.of(2026, 11, 28)
        val todayMs = today.atTime(10, 0).toInstant(zone).toEpochMilli()
        val yesterdayMs = today.minusDays(1).atTime(18, 0).toInstant(zone).toEpochMilli()
        val twoDaysAgoMs = today.minusDays(2).atTime(9, 0).toInstant(zone).toEpochMilli()
        val fourDaysAgoMs = today.minusDays(4).atTime(9, 0).toInstant(zone).toEpochMilli()

        val attempts = listOf(
            QuizAttemptRecord(id = "a1", scorePercent = 80L, pointsEarned = 20L, attemptedAtMillis = todayMs),
            QuizAttemptRecord(id = "a2", scorePercent = 100L, pointsEarned = 30L, attemptedAtMillis = yesterdayMs),
            QuizAttemptRecord(id = "a3", scorePercent = 60L, pointsEarned = 10L, attemptedAtMillis = fourDaysAgoMs)
        )
        val reads = listOf(
            ReadingProgressRecord(id = "r1", materialId = "m1", progressPercent = 100L, updatedAtMillis = todayMs),
            ReadingProgressRecord(id = "r2", materialId = "m2", progressPercent = 100L, updatedAtMillis = twoDaysAgoMs),
            ReadingProgressRecord(id = "r3", materialId = "m3", progressPercent = 50L, updatedAtMillis = todayMs)
        )

        val stats = UserStatsCalculator.compute(
            quizAttempts = attempts,
            readingProgress = reads,
            nowMillis = todayMs + 3_600_000L,
            zoneId = zone
        )

        // Average score: (80 + 100 + 60) / 3 = 80
        assertEquals(80L, stats.avgScore)
        // Points: (20 + 30 + 10) quiz pts + (2 completed reads * 5 pts) = 70
        assertEquals(70L, stats.points)
        // Daily completed today: 1 quiz today (a1) + 1 100%-read today (r1) = 2 (r3 is 50%, doesn't count)
        assertEquals(2L, stats.dailyCompletedCount)
        // Streak: today, yesterday, twoDaysAgo => 3 consecutive days (fourDaysAgo has a gap on day -3)
        assertEquals(3L, stats.dayStreak)
    }

    @Test
    fun `room replace user subcollection methods clear stale records when cloud returns empty`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, StudyHubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.studyHubDao()
        val uid = "student_1"

        try {
            dao.upsertBookmarks(
                listOf(CachedBookmarkEntity("b1", uid, "m1", "Title", "Science", 7L, "short_notes", "Sub", "blue", 100L))
            )
            dao.upsertQuizAttempts(
                listOf(CachedQuizAttemptEntity("q1", uid, "qz1", "Quiz", "Science", 7L, "practice", 5L, 4L, 80L, 30L, 40L, 100L))
            )
            dao.upsertReadingProgressList(
                listOf(CachedReadingProgressEntity("p1", uid, "m1", "Title", "Science", "Ch1", 7L, 100L, 100L))
            )
            dao.upsertDownloads(
                listOf(CachedDownloadEntity("d1", uid, "m1", "Title", "Science", 7L, "1 MB", 100L))
            )

            dao.replaceUserBookmarks(uid, emptyList())
            dao.replaceUserQuizAttempts(uid, emptyList())
            dao.replaceUserReadingProgress(uid, emptyList())
            dao.replaceUserDownloads(uid, emptyList())

            assertTrue(dao.observeBookmarks(uid).first().isEmpty())
            assertTrue(dao.observeQuizAttempts(uid).first().isEmpty())
            assertTrue(dao.observeReadingProgress(uid).first().isEmpty())
            assertTrue(dao.observeDownloads(uid).first().isEmpty())
        } finally {
            db.close()
        }
    }
}
