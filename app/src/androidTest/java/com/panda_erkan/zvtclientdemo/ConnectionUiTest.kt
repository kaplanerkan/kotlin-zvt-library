package com.panda_erkan.zvtclientdemo

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.panda_erkan.zvtclientdemo.ui.main.MainActivity

@RunWith(AndroidJUnit4::class)
class ConnectionUiTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun hostField_isDisplayed() {
        onView(withId(R.id.etHost))
            .check(matches(isDisplayed()))
    }

    @Test
    fun portField_isDisplayed() {
        onView(withId(R.id.etPort))
            .check(matches(isDisplayed()))
    }

    @Test
    fun hostField_hasDefaultText() {
        onView(withId(R.id.etHost))
            .check(matches(withText("192.168.1.135")))
    }

    @Test
    fun portField_hasDefaultText() {
        onView(withId(R.id.etPort))
            .check(matches(withText("20007")))
    }

    @Test
    fun connectButton_isDisplayed() {
        onView(withId(R.id.btnConnect))
            .check(matches(isDisplayed()))
    }

    @Test
    fun simulatorSwitch_isDisplayedAndNotChecked() {
        onView(withId(R.id.switchSimulator))
            .check(matches(isDisplayed()))
            .check(matches(isNotChecked()))
    }
}
