package com.hsissa.zentra.service

import android.content.Context
import androidx.room.withTransaction
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.hsissa.zentra.data.local.AppDatabase
import com.hsissa.zentra.data.local.FocusSessionEntity
import com.hsissa.zentra.data.local.FocusStreakEntity
import com.hsissa.zentra.core.FocusStreakCalculator
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** Durable state shared by the timer, its recovery worker, and the quick-settings tile. */
class FocusSessionStore(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val prefs = context.getSharedPreferences("focus_session_state", Context.MODE_PRIVATE)

    data class Session(val id: Long, val end: Long, val minutes: Int, val isBreak: Boolean = false)

    fun active(): Session? {
        val id = prefs.getLong("id", 0L)
        return if (id == 0L) null else Session(id, prefs.getLong("end", 0L), prefs.getInt("minutes", 25), prefs.getBoolean("break", false))
    }

    fun start(minutes: Int, isBreak: Boolean = false): Session {
        require(minutes in 1..1440)
        val now = nowMillis()
        val session = Session(now, now + minutes * 60_000L, minutes, isBreak)
        check(prefs.edit().putLong("id", session.id).putLong("end", session.end).putInt("minutes", minutes).putBoolean("break", isBreak).commit())
        val work = OneTimeWorkRequestBuilder<FocusCompletionWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .setInputData(workDataOf("session_id" to session.id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, work)
        return session
    }

    fun cancel() {
        prefs.edit().clear().commit()
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    suspend fun complete(id: Long): Boolean {
        val session = active()?.takeIf { it.id == id && nowMillis() >= it.end } ?: return false
        database.withTransaction {
            val dao = database.usageDao()
            if (dao.getFocusSession(id) == null) {
                dao.insertFocusSession(FocusSessionEntity(id = id, durationMinutes = session.minutes,
                    timestamp = session.end, completed = true,
                    sessionType = if (session.isBreak) "Break" else if (session.minutes >= 45) "Deep Focus" else "Pomodoro"))
                if (!session.isBreak) {
                    val old = dao.getFocusStreak() ?: FocusStreakEntity()
                    val zone = ZoneId.systemDefault()
                    val date = Instant.ofEpochMilli(session.end).atZone(zone).toLocalDate()
                    val previous = old.lastGoalMetTimestamp.takeIf { it > 0 }
                        ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
                    val streak = FocusStreakCalculator.next(old.currentStreak, previous, date)
                    dao.insertFocusStreak(old.copy(currentStreak = streak, longestStreak = maxOf(old.longestStreak, streak),
                        totalFocusMinutes = old.totalFocusMinutes + session.minutes, lastGoalMetTimestamp = session.end))
                }
            }
        }
        if (active()?.id == id) prefs.edit().clear().commit()
        return true
    }

    companion object { private const val WORK_NAME = "focus_completion" }
}
