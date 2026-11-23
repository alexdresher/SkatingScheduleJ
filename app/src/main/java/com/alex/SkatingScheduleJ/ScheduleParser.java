package com.alex.SkatingScheduleJ;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduleParser {

    public List<ScheduleItem> parse(String inputText, boolean afterTimeEnabled, int afterHour) {
        List<ScheduleItem> items = new ArrayList<>();
        boolean parserAfterTimeEnabled=afterTimeEnabled;
        int parserAfterHour=afterHour;
        String[] lines = inputText.trim().split("\n");

        // Извлекаем диапазон дат из первой строки
        DateRange range = parseDateRange(lines[0]);

        int startDay;
        int startMonth;
        if (range != null) {
            startDay = range.startDay;
            startMonth = range.startMonth;
            } else {
            //Берём по умолчанию текущий понедельник
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            startDay = calendar.get(Calendar.DAY_OF_MONTH);
            startMonth = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH возвращает 0-11
        }

        Map<String, Integer> dayOfWeekMap = new HashMap<>();
        dayOfWeekMap.put("Пн", Calendar.MONDAY);
        dayOfWeekMap.put("Вт", Calendar.TUESDAY);
        dayOfWeekMap.put("Ср", Calendar.WEDNESDAY);
        dayOfWeekMap.put("Чт", Calendar.THURSDAY);
        dayOfWeekMap.put("Пт", Calendar.FRIDAY);
        dayOfWeekMap.put("Сб", Calendar.SATURDAY);
        dayOfWeekMap.put("Вс", Calendar.SUNDAY);

        Map<String, Integer> dayNumbers = new HashMap<>();
        dayNumbers.put("Пн", 1);
        dayNumbers.put("Вт", 2);
        dayNumbers.put("Ср", 3);
        dayNumbers.put("Чт", 4);
        dayNumbers.put("Пт", 5);
        dayNumbers.put("Сб", 6);
        dayNumbers.put("Вс", 7);

        String currentDayOfWeek = "";
        String currentDate = "";
        boolean isDayOff = false;
        boolean isYudinoActive = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Удаляем восклицательный знак из начала строки (❗️ или ❗)
            trimmed = trimmed.replaceAll("[❗️!]", "");

            // Проверка на день недели
            java.util.regex.Pattern dayPattern = java.util.regex.Pattern.compile("^(Пн|Вт|Ср|Чт|Пт|Сб|Вс)\\s*(.*)");
            java.util.regex.Matcher dayMatcher = dayPattern.matcher(trimmed);

            if (dayMatcher.find()) {
                currentDayOfWeek = dayMatcher.group(1);
                String restOfLine = dayMatcher.group(2); // то, что идёт после дня недели
                isDayOff = trimmed.toLowerCase().contains("выходной");
                currentDate = calculateDate(startDay, startMonth, currentDayOfWeek, dayOfWeekMap);

                // СБРАСЫВАЕМ Юдино при начале нового дня

                // Проверяем, есть ли слово "Юдино" в строке дня
                // Если да, то включаем Юдино для этой строки и последующих
                isYudinoActive = restOfLine.toLowerCase().contains("юдино");

                if (isDayOff) {
                    items.add(new ScheduleItem(currentDate + " " + currentDayOfWeek, "Выходной", "", false));
                }

                // Проверяем занятие в той же строке
                java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile(
                        "(\\d{1,2}[.:]\\d{2})[-–—](\\d{1,2}[.:]\\d{2})\\s+(.+)");
                java.util.regex.Matcher timeMatcher = timePattern.matcher(restOfLine);

                if (timeMatcher.find() && !isDayOff) {
                    String startTime = Objects.requireNonNull(timeMatcher.group(1)).replace(".", ":");
                    String endTime = Objects.requireNonNull(timeMatcher.group(2)).replace(".", ":");
                    String timeRange = startTime + " - " + endTime;
                    int startHour = Integer.parseInt(startTime.split(":")[0]);
                    String lessonNames = timeMatcher.group(3);
                    addLessonItemsSeparately(items, currentDate, currentDayOfWeek, timeRange, lessonNames, startHour, dayNumbers.get(currentDayOfWeek), isYudinoActive, parserAfterTimeEnabled, parserAfterHour);
                }
                continue;
            }

            // Проверяем, есть ли Юдино в строке
            boolean lineHasYudino = trimmed.toLowerCase().contains("юдино");

            if (lineHasYudino) {
                isYudinoActive = true;
                // Удаляем слово "Юдино" из строки
                trimmed = trimmed.toLowerCase().replaceFirst("юдино", "").trim();
            }

            // Обработка строк с занятиями
            //java.util.regex.Pattern lessonPattern = java.util.regex.Pattern.compile("^(\\d{1,2}\\.\\d{2})-(\\d{1,2}\\.\\d{2})\\s+(.+)");
            java.util.regex.Pattern lessonPattern = java.util.regex.Pattern.compile("^(\\d{1,2}[.:]\\d{2})[-–—](\\d{1,2}[.:]\\d{2})\\s+(.+)");
            java.util.regex.Matcher lessonMatcher = lessonPattern.matcher(trimmed);

            if (lessonMatcher.find() && !currentDayOfWeek.isEmpty() && !isDayOff) {
                String startTime = Objects.requireNonNull(lessonMatcher.group(1)).replace(".", ":");
                String endTime = Objects.requireNonNull(lessonMatcher.group(2)).replace(".", ":");
                String timeRange = startTime + " - " + endTime;
                int startHour = Integer.parseInt(startTime.split(":")[0]);
                String lessonNames = lessonMatcher.group(3);
                addLessonItemsSeparately(items, currentDate, currentDayOfWeek, timeRange, lessonNames, startHour, dayNumbers.get(currentDayOfWeek), isYudinoActive, parserAfterTimeEnabled, parserAfterHour);
            }
        }

        return items;
    }

    /**
     * Диапазон дат из заголовка расписания.
     */
    public static final class DateRange {
        public final int startDay;
        public final int startMonth;
        public final int endDay;
        public final int endMonth;

        public DateRange(int startDay, int startMonth, int endDay, int endMonth) {
            this.startDay = startDay;
            this.startMonth = startMonth;
            this.endDay = endDay;
            this.endMonth = endMonth;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DateRange)) return false;
            DateRange r = (DateRange) o;
            return startDay == r.startDay && startMonth == r.startMonth
                    && endDay == r.endDay && endMonth == r.endMonth;
        }

        @Override
        public int hashCode() {
            return Objects.hash(startDay, startMonth, endDay, endMonth);
        }

        @Override
        public String toString() {
            return startDay + "." + startMonth + "-" + endDay + "." + endMonth;
        }
    }

    // Возможные форматы первой строки:
    // "Расписание 01.07-03.07"
    // "Расписание 23-29.11"
    // "Расписание 6-10 июля"
    // "Расписание с 1 по 3 июля"
    private static final Pattern[] DATE_PATTERNS = {
            // 01.07-03.07
            Pattern.compile("(\\d{1,2})\\.(\\d{1,2})[-–—](\\d{1,2})\\.(\\d{1,2})"),
            // 23-29.11 (месяц только у второй даты)
            Pattern.compile("(\\d{1,2})[-–—](\\d{1,2})\\.(\\d{1,2})"),
            // 6-10 июля
            Pattern.compile("(\\d{1,2})[-–—](\\d{1,2})\\s([а-яА-ЯёЁ])"),
            // с 1 по 3 июля
            Pattern.compile("с\\s(\\d{1,2})\\sпо\\s(\\d{1,2})\\s([а-яА-ЯёЁ])")
    };

    /**
     * Разбирает строку-заголовок с диапазоном дат.
     *
     * @return диапазон или null, если дата в строке не найдена
     * (тогда parse() берёт текущий понедельник)
     */
    DateRange parseDateRange(String headerLine) {
        if (headerLine == null) return null;

        for (int i = 0; i < DATE_PATTERNS.length; i++) {
            Matcher m = DATE_PATTERNS[i].matcher(headerLine);
            if (!m.find()) continue;

            if (i == 0) {
                // 01.07-03.07
                return new DateRange(
                        Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
                        Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)));
            }
            if (i == 1) {
                // 23-29.11; если день начала больше дня конца (28-4.12),
                // значит неделя начинается в предыдущем месяце
                int startDay = Integer.parseInt(m.group(1));
                int endDay = Integer.parseInt(m.group(2));
                int endMonth = Integer.parseInt(m.group(3));
                int startMonth = endMonth;
                if (startDay > endDay) {
                    startMonth = endMonth == 1 ? 12 : endMonth - 1;
                }
                return new DateRange(startDay, startMonth, endDay, endMonth);
            }
            // 6-10 июля / с 1 по 3 июля
            int month = monthToNumber(m.group(3));
            return new DateRange(
                    Integer.parseInt(m.group(1)), month,
                    Integer.parseInt(m.group(2)), month);
        }
        return null;
    }

    private int monthToNumber(String month) {
        return switch (month.toLowerCase()) {
            case "января" -> 1;
            case "февраля" -> 2;
            case "марта" -> 3;
            case "апреля" -> 4;
            case "мая" -> 5;
            case "июня" -> 6;
            case "июля" -> 7;
            case "августа" -> 8;
            case "сентября" -> 9;
            case "октября" -> 10;
            case "ноября" -> 11;
            case "декабря" -> 12;
            default -> throw new IllegalArgumentException(
                    "Неизвестный месяц: " + month
            );
        };
    }

    private void addLessonItemsSeparately(List<ScheduleItem> items, String currentDate, String currentDayOfWeek,
                                          String timeRange, String lessonNames, int startHour, Integer dayNumber, boolean isYudinoActive, boolean afterTimeEnabled, int afterHour) {
        if (dayNumber == null) dayNumber = 0;

        if (lessonNames.contains(",")) {
            // Разделяем занятия по запятым
            // Если это одно занятие, то дублируем
            String[] lessons = lessonNames.split(",");
            String currentBasePrefix = "";
            for (String lesson : lessons) {
                String trimmedLesson = lesson.trim().toLowerCase();
                int spaceIndex = trimmedLesson.indexOf(' ');
                if (spaceIndex != -1) {
                    // Если в строке есть пробел (например, "лед ст"),
                    // запоминаем первое слово как базу ("лед")
                    currentBasePrefix = trimmedLesson.substring(0, spaceIndex);
                } else if (!currentBasePrefix.isEmpty()) {
                    // Если пробела нет (одиночное слово, например, "мл"),
                    // дописываем спереди ранее сохраненную базу -> "лед мл"
                    trimmedLesson = currentBasePrefix + " " + trimmedLesson;
                }
                String normalizedLesson = normalizeLessonName(trimmedLesson);
                if (!normalizedLesson.isEmpty()) {
                    // ДЛЯ КАЖДОГО ЗАНЯТИЯ ВЫЧИСЛЯЕМ isSelected ИНДИВИДУАЛЬНО
                    boolean isSelected = shouldBeSelectedByDefault(startHour, trimmedLesson, dayNumber, afterTimeEnabled, afterHour);
                    items.add(new ScheduleItem(currentDate + " " + currentDayOfWeek, timeRange, normalizedLesson, isSelected, isYudinoActive));
                }
            }
        } else {
            String normalizedLesson = normalizeLessonName(lessonNames.toLowerCase());
            if (!normalizedLesson.isEmpty()) {
                boolean isSelected = shouldBeSelectedByDefault(startHour, lessonNames, dayNumber, afterTimeEnabled, afterHour);
                items.add(new ScheduleItem(currentDate + " " + currentDayOfWeek, timeRange, normalizedLesson, isSelected, isYudinoActive));
            }
        }
    }

    private String normalizeLessonName(String lessonName) {
        String name = lessonName.trim().toLowerCase();

        if (name.isEmpty() || name.equals("нет занятий")) {
            return "";
        }

        String returnName="";
        if (lessonName.contains("лед") || lessonName.contains(("лёд"))) returnName="Лёд";
        if (lessonName.contains("раст")) returnName="Раст";
        if (lessonName.contains("хор")) returnName="Хор";
        if (lessonName.contains("офп")) returnName="ОФП";
        if (lessonName.contains("сфп")) returnName="СФП";
        if (returnName.isEmpty()) {
            returnName = lessonName;
        }
        else {
            if (lessonName.contains(" ст") || lessonName.startsWith(("ст "))) returnName += " ст";
            if (lessonName.contains(" мл") || lessonName.contains(("мл "))) returnName += " мл";
        }

        return returnName;
    }

    private boolean shouldBeSelectedByDefault(int startHour, String lessonName, Integer dayNumber,
                                              boolean afterTimeEnabled, int afterHour) {
        // Будние дни до порога времени - не выделяем (только если фильтр включен)
        if (afterTimeEnabled && dayNumber != null && dayNumber >= 1 && dayNumber <= 5 && startHour < afterHour) {
            return false;
        }

        // Если есть "мл" - не выделяем
        return !lessonName.contains("мл");
    }

    private String calculateDate(int startDay, int startMonth, String dayOfWeek, Map<String, Integer> dayOfWeekMap) {
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        calendar.set(currentYear, startMonth - 1, startDay);

        int startDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        Integer targetDayOfWeek = dayOfWeekMap.get(dayOfWeek);
        if (targetDayOfWeek == null) targetDayOfWeek = Calendar.MONDAY;

        int diff = targetDayOfWeek - startDayOfWeek;
        if (diff < 0) diff += 7;

        calendar.add(Calendar.DAY_OF_YEAR, diff);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }
}