package com.alex.SkatingScheduleJ;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ScheduleParserTest {

    private ScheduleParser parser;

    @Before
    public void setUp() {
        parser = new ScheduleParser();
    }

    @Test
    public void testParseBasicSchedule() {
        String input = "Расписание 27.04-03.05:\n" +
                "Пн 8.15-9.15 лёд\n" +
                "      14.30-15.30 мл ОФП\n" +
                "      15.45-16.45 лёд\n" +
                "      17.00-18.00 ст хор";

        List<ScheduleItem> items = parser.parse(input);

        assertNotNull(items);
        assertEquals(4, items.size());

        ScheduleItem first = items.get(0);
        assertEquals("27.04 Пн", first.getDayAndNumber());
        assertEquals("8:15 - 9:15", first.getTimeRange());
        assertEquals("Лёд", first.getSelectedLesson());
    }

    @Test
    public void testParseWithCommas() {
        String input = "Расписание 27.04-03.05:\n" +
                "Вт 15.45-16.45 хор мл, СФП ст\n" +
                "      16.45-17.45 хор ст, офп мл";

        List<ScheduleItem> items = parser.parse(input);

        assertNotNull(items);
        assertEquals(4, items.size());

        // Проверяем разделение по запятым
        assertEquals("Хор мл", items.get(0).getSelectedLesson());
        assertEquals("СФП ст", items.get(1).getSelectedLesson());
        assertEquals("Хор ст", items.get(2).getSelectedLesson());
        assertEquals("ОФП мл", items.get(3).getSelectedLesson());
    }

    @Test
    public void testParseWithYudino() {
        String input = "Расписание 27.04-03.05:\n" +
                "Ср 8.15-9.15 лёд\n" +
                "Юдино 16.15-17.15 офп ст\n" +
                "       17.15-18.15 СФП мл, раст ст\n" +
                "       18.30-19.30 лёд";

        List<ScheduleItem> items = parser.parse(input);

        assertNotNull(items);

        // Первое занятие без Юдино
        assertFalse(items.get(0).isYudino());

        // Остальные с Юдино
        for (int i = 1; i < items.size(); i++) {
            assertTrue(items.get(i).isYudino());
        }
    }

    @Test
    public void testParseWithExclamationMark() {
        String input = "Расписание 27.04-03.05:\n" +
                "❗️Чт 8.15-9.15 лёд\n" +
                "      15.30-16.30 лёд\n" +
                "❗️Пт 12.30-13.30 хор мл";

        List<ScheduleItem> items = parser.parse(input);

        assertNotNull(items);
        assertEquals(3, items.size());
        assertEquals("Чт", items.get(0).getDayAndNumber().split(" ")[1]);
        assertEquals("Пт", items.get(2).getDayAndNumber().split(" ")[1]);
    }

    @Test
    public void testParseWithDayOff() {
        String input = "Расписание 27.04-03.05:\n" +
                "Ср Выходной. Тесты";

        List<ScheduleItem> items = parser.parse(input);

        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("Выходной", items.get(0).getTimeRange());
    }

    @Test
    public void testParseTimeRange() {
        String input = "Расписание 27.04-03.05:\n" +
                "Пн 8.15-9.15 лёд\n" +
                "      14.30-15.30 мл ОФП";

        List<ScheduleItem> items = parser.parse(input);

        assertEquals("8:15 - 9:15", items.get(0).getTimeRange());
        assertEquals("14:30 - 15:30", items.get(1).getTimeRange());
    }

    @Test
    public void testNormalizeLessonNames() {
        String input = "Расписание 27.04-03.05:\n" +
                "Сб 10.00-11.00 ст хор\n" +
                "      11.15-12.15 мл лёд\n" +
                "      12.30-13.30 раст ст";

        List<ScheduleItem> items = parser.parse(input);

        assertEquals("Хор ст", items.get(0).getSelectedLesson());
        assertEquals("Лёд мл", items.get(1).getSelectedLesson());
        assertEquals("Раст ст", items.get(2).getSelectedLesson());
    }

    @Test
    public void testDefaultSelection() {
        String input = "Расписание 27.04-03.05:\n" +
                "Пн 8.15-9.15 лёд\n" +           // будний день до 12 → false
                "Пн 14.30-15.30 хор ст\n" +       // есть "ст" → true
                "Пн 15.45-16.45 офп мл\n" +       // есть "мл" → false
                "Сб 9.00-10.00 лёд ст\n";        // выходной → true

        List<ScheduleItem> items = parser.parse(input);

        assertFalse(items.get(0).isSelected());
        assertTrue(items.get(1).isSelected());
        assertFalse(items.get(2).isSelected());
        assertTrue(items.get(3).isSelected());
    }
    @Test
    public void testUnknownLesson() {
        String input = "Расписание 25.05-27.05:\n" +
                "Пн 15.45-16.45 мл лед\n" +
                "17.00-18.00 откр урок мл\n" + //откр урок мл, не выбрано
                "16.45-17.45 лёд ст\n" +
                "18.00-19.00 откр урок ст\n" + //откр урок ст, выбрано
                "Ср 9.30-10.30 тесты\n" + //тесты, не выбрано
                "10.45-11.45 лёд\n" +
                "12.00-13.00 откр урок ст\n";

        List<ScheduleItem> items = parser.parse(input);

        assertEquals("откр урок мл", items.get(1).getSelectedLesson());
        assertFalse(items.get(1).isSelected());
        assertEquals("откр урок ст", items.get(3).getSelectedLesson());
        assertTrue(items.get(3).isSelected());
        assertEquals("тесты", items.get(4).getSelectedLesson());
        assertFalse(items.get(4).isSelected());
    }
    @Test
    public void testErrorLesson() {
        String input = "Расписание 13.07-19.07:\n" +
                "Вс 8.30–9.30 ст офп\n" +//пропал
                "9.45-10.45 ст лед\n" +
                "10.45-11.45 мл лед\n" +
                "12.00-13.00 мл раст\n";

        List<ScheduleItem> items = parser.parse(input);

        assertEquals("ОФП ст", items.get(0).getSelectedLesson());
        assertTrue(items.get(0).isSelected());
        assertEquals("Лёд ст", items.get(1).getSelectedLesson());
        assertTrue(items.get(1).isSelected());
        assertFalse(items.get(2).isSelected());
        assertFalse(items.get(3).isSelected());
    }
}