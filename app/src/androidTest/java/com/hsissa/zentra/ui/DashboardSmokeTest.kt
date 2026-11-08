package com.hsissa.zentra.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hsissa.zentra.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardSmokeTest {
    @Test fun focusDurationAndReminderSettingsAreReachable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.btnFocusToggle)).perform(scrollTo(), click())
            onView(withText(R.string.focus_choose_duration)).check(matches(isDisplayed()))
            pressBack()
            onView(withId(R.id.navigation_settings)).perform(click())
            onView(withId(R.id.btnManageLimits)).check(matches(isDisplayed()))
        }
    }
}
