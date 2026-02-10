package com.panda_erkan.zvtclientdemo

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.panda_erkan.zvtclientdemo.ui.main.MainActivity

@RunWith(AndroidJUnit4::class)
class MainNavigationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun toolbar_isDisplayed() {
        onView(withId(R.id.toolbar))
            .check(matches(isDisplayed()))
    }

    @Test
    fun bottomNav_isDisplayed() {
        onView(withId(R.id.bottomNav))
            .check(matches(isDisplayed()))
    }

    @Test
    fun connectionCard_hostField_isDisplayed() {
        onView(withId(R.id.etHost))
            .check(matches(isDisplayed()))
    }

    @Test
    fun connectionState_isDisplayed() {
        onView(withId(R.id.tvConnectionState))
            .check(matches(isDisplayed()))
    }

    @Test
    fun bottomNav_hasItems_isDisplayed() {
        onView(withId(R.id.bottomNav))
            .check(matches(isDisplayed()))
    }
}
