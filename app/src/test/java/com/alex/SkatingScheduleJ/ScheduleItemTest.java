package com.alex.SkatingScheduleJ;

import org.junit.Test;

import static org.junit.Assert.*;

public class ScheduleItemTest {

    @Test
    public void testScheduleItemCreation() {
        ScheduleItem item = new ScheduleItem("27.04 Пн", "8:15 - 9:15", "лёд", true, false);

        assertEquals("27.04 Пн", item.getDayAndNumber());
        assertEquals("8:15 - 9:15", item.getTimeRange());
        assertEquals("лёд", item.getSelectedLesson());
        assertTrue(item.isSelected());
        assertFalse(item.isYudino());
    }

    @Test
    public void testSettersAndGetters() {
        ScheduleItem item = new ScheduleItem("", "", "", false, false);

        item.setDayAndNumber("28.04 Вт");
        item.setTimeRange("10:00 - 11:00");
        item.setSelectedLesson("хор ст");
        item.setSelected(true);
        item.setYudino(true);

        assertEquals("28.04 Вт", item.getDayAndNumber());
        assertEquals("10:00 - 11:00", item.getTimeRange());
        assertEquals("хор ст", item.getSelectedLesson());
        assertTrue(item.isSelected());
        assertTrue(item.isYudino());
    }
}