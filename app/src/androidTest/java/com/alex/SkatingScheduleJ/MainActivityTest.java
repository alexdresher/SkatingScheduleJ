package com.alex.SkatingScheduleJ;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testMainActivityIsDisplayed() {
        onView(withId(R.id.processButton)).check(matches(isDisplayed()));
        onView(withId(R.id.inputText)).check(matches(isDisplayed()));
    }

    @Test
    public void testProcessButtonWithEmptyText() {
        onView(withId(R.id.processButton)).perform(click());
        // Должен появиться Toast
    }

    @Test
    public void testProcessButtonWithText() {
        String schedule = "Расписание 27.04-03.05:\nПн 8.15-9.15 лёд";
        onView(withId(R.id.inputText)).perform(typeText(schedule));
        onView(withId(R.id.processButton)).perform(click());
        // Должен открыться ScheduleTableActivity
    }
}