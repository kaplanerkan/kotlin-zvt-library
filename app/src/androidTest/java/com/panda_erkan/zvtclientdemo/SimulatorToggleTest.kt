package com.panda_erkan.zvtclientdemo

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.panda_erkan.zvtclientdemo.ui.main.MainActivity

@RunWith(AndroidJUnit4::class)
class SimulatorToggleTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun simulatorSwitch_isNotCheckedByDefault() {
        onView(withId(R.id.switchSimulator))
            .check(matches(isDisplayed()))
            .check(matches(isNotChecked()))
    }

    @Test
    fun simulatorHint_isGoneByDefault() {
        onView(withId(R.id.tvSimulatorHint))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun simulatorSwitch_afterClick_isChecked() {
        onView(withId(R.id.switchSimulator))
            .perform(click())
        onView(withId(R.id.switchSimulator))
            .check(matches(isChecked()))
    }

    @Test
    fun keepAliveSwitch_isCheckedByDefault() {
        onView(withId(R.id.cbKeepAlive))
            .check(matches(isDisplayed()))
            .check(matches(isChecked()))
    }
}
