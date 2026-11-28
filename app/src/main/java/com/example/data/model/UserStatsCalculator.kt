package com.example.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ComputedUserStats(
    val avgScore: Long = 0L,
    val dayStreak: Long = 0L,
    val points: Long = 0L,
    val dailyCompletedCount: Long = 0L
)

object UserStatsCalculator {

    fun compute(
        quizAttempts: List<QuizAttemptRecord>,
        readingProgress: List<ReadingProgressRecord>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ComputedUserStats {
        val avgScore = if (quizAttempts.isEmpty()) {
            0L
        } else {
            (quizAttempts.sumOf { it.scorePercent.coerceIn(0L, 100L) } / quizAttempts.size).coerceIn(0L, 100L)
        }

        val quizPoints = quizAttempts.sumOf { it.pointsEarned.coerceAtLeast(0L) }
        val completedReadCount = readingProgress.count { it.progressPercent >= 100L }.toLong()
        val totalPoints = (quizPoints + completedReadCount * 5L).coerceAtLeast(0L)

        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

        val attemptDates = quizAttempts.mapNotNull { attempt ->
            val ts = resolveTimestampMillis(attempt.attemptedAtMillis, attempt.createdAt, attempt.updatedAt)
            if (ts > 0L) Instant.ofEpochMilli(ts).atZone(zoneId).toLocalDate() else null
        }

        val readDates = readingProgress
            .filter { it.progressPercent > 0L }
            .mapNotNull { progress ->
                val ts = resolveTimestampMillis(progress.updatedAtMillis, progress.updatedAt, progress.createdAt)
                if (ts > 0L) Instant.ofEpochMilli(ts).atZone(zoneId).toLocalDate() else null
            }

        val completedReadDatesToday = readingProgress
            .filter { it.progressPercent >= 100L }
            .count { progress ->
                val ts = resolveTimestampMillis(progress.updatedAtMillis, progress.updatedAt, progress.createdAt)
                ts > 0L && Instant.ofEpochMilli(ts).atZone(zoneId).toLocalDate() == today
            }

        val todayQuizCount = attemptDates.count { it == today }
        val dailyCompletedCount = (todayQuizCount + completedReadDatesToday).toLong().coerceIn(0L, 100L)

        val allActivityDates: Set<LocalDate> = (attemptDates + readDates).toSet()
        val dayStreak = calculateStreak(allActivityDates, today)

        return ComputedUserStats(
            avgScore = avgScore,
            dayStreak = dayStreak,
            points = totalPoints,
            dailyCompletedCount = dailyCompletedCount
        )
    }

    private fun resolveTimestampMillis(
        cachedMillis: Long,
        primaryIso: String?,
        secondaryIso: String?
    ): Long {
        if (cachedMillis > 0L) return cachedMillis
        return SupabaseMappers.parseIsoToMillisOrNull(primaryIso)
            ?: SupabaseMappers.parseIsoToMillisOrNull(secondaryIso)
            ?: 0L
    }

    private fun calculateStreak(dates: Set<LocalDate>, today: LocalDate): Long {
        if (dates.isEmpty()) return 0L
        val anchor = when {
            today in dates -> today
            today.minusDays(1) in dates -> today.minusDays(1)
            else -> return 0L
        }
        var streak = 0L
        var cursor = anchor
        while (cursor in dates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
