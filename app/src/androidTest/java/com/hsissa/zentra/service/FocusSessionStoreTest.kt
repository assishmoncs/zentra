package com.hsissa.zentra.service

import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hsissa.zentra.data.local.AppDatabase
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusSessionStoreTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var store: FocusSessionStore
    private val today = LocalDate.of(2026, 11, 7).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Before fun setUp() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String?, mode: Int) =
                base.getSharedPreferences("focus_store_test_$name", mode)
        }
        context.getSharedPreferences("focus_session_state", Context.MODE_PRIVATE).edit().clear().commit()
        database = Room.inMemoryDatabaseBuilder(base, AppDatabase::class.java).build()
        store = FocusSessionStore(context, database) { today + 86_400_000L }
    }

    @After fun tearDown() {
        context.getSharedPreferences("focus_session_state", Context.MODE_PRIVATE).edit().clear().commit()
        database.close()
    }

    private fun active(id: Long, end: Long, isBreak: Boolean = false) {
        context.getSharedPreferences("focus_session_state", Context.MODE_PRIVATE).edit()
            .putLong("id", id).putLong("end", end).putInt("minutes", 25).putBoolean("break", isBreak).commit()
    }

    @Test fun completionIsIdempotentAndBreaksDoNotIncreaseStreak() = runTest {
        active(101, today)
        assertTrue(store.complete(101))
        active(101, today) // Simulate a worker replay after persistence but before state cleanup.
        assertTrue(store.complete(101))
        assertEquals(1, database.usageDao().getAllFocusSessions().size)
        assertEquals(25L, database.usageDao().getFocusStreak()?.totalFocusMinutes)
        active(102, today, isBreak = true)
        assertTrue(store.complete(102))
        assertEquals(1, database.usageDao().getFocusStreak()?.currentStreak)
        assertEquals(25L, database.usageDao().getFocusStreak()?.totalFocusMinutes)
    }

    @Test fun futureSessionIsNotCompletedEarly() = runTest {
        active(103, today + 2 * 86_400_000L)
        assertFalse(store.complete(103))
        assertTrue(database.usageDao().getAllFocusSessions().isEmpty())
    }
}
