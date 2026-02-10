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
class PaymentUiTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun amountInput_isDisplayed() {
        onView(withId(R.id.etAmount))
            .check(matches(isDisplayed()))
    }

    @Test
    fun authorizeButton_isDisplayed() {
        onView(withId(R.id.btnAuthorize))
            .check(matches(isDisplayed()))
    }

    @Test
    fun refundButton_isDisplayed() {
        onView(withId(R.id.btnRefund))
            .check(matches(isDisplayed()))
    }

    @Test
    fun reversalButton_isDisplayed() {
        onView(withId(R.id.btnReversal))
            .check(matches(isDisplayed()))
    }

    @Test
    fun preAuthButton_isDisplayed() {
        onView(withId(R.id.btnPreAuth))
            .check(matches(isDisplayed()))
    }
}
