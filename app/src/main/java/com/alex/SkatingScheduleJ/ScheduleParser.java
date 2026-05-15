package com.alex.SkatingScheduleJ;

import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduleParser {

    public List<ScheduleItem> parse(String inputText) {
        List<ScheduleItem> items = new ArrayList<>();
        String[] lines = inputText.trim().split("\n");

        // Извлекаем диапазон дат
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{1,2})\\.(\\d{1,2})-(\\d{1,2})\\.(\\d{1,2})");
        java.util.regex.Matcher dateMatcher = datePattern.matcher(lines[0]);

        //Берём по умолчанию текущий понедельник
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        int startDay = calendar.get(Calendar.DAY_OF_MONTH);
        int startMonth = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH возвращает 0-11

        if (dateMatcher.find()) {
            startDay = Integer.parseInt(Objects.requireNonNull(dateMatcher.group(1)));
            startMonth = Integer.parseInt(Objects.requireNonNull(dateMatcher.group(2)));
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
                        "(\\d{1,2}[.:]\\d{2})-(\\d{1,2}[.:]\\d{2})\\s+(.+)");
                java.util.regex.Matcher timeMatcher = timePattern.matcher(restOfLine);

                if (timeMatcher.find() && !isDayOff) {
                    String startTime = Objects.requireNonNull(timeMatcher.group(1)).replace(".", ":");
                    String endTime = Objects.requireNonNull(timeMatcher.group(2)).replace(".", ":");
                    String timeRange = startTime + " - " + endTime;
                    int startHour = Integer.parseInt(startTime.split(":")[0]);
                    String lessonNames = timeMatcher.group(3);
                    addLessonItemsSeparately(items, currentDate, currentDayOfWeek, timeRange, lessonNames, startHour, dayNumbers.get(currentDayOfWeek), isYudinoActive);
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
            java.util.regex.Pattern lessonPattern = java.util.regex.Pattern.compile("^(\\d{1,2}\\.\\d{2})-(\\d{1,2}\\.\\d{2})\\s+(.+)");
            java.util.regex.Matcher lessonMatcher = lessonPattern.matcher(trimmed);

            if (lessonMatcher.find() && !currentDayOfWeek.isEmpty() && !isDayOff) {
                String startTime = Objects.requireNonNull(lessonMatcher.group(1)).replace(".", ":");
                String endTime = Objects.requireNonNull(lessonMatcher.group(2)).replace(".", ":");
                String timeRange = startTime + " - " + endTime;
                int startHour = Integer.parseInt(startTime.split(":")[0]);
                String lessonNames = lessonMatcher.group(3);
                addLessonItemsSeparately(items, currentDate, currentDayOfWeek, timeRange, lessonNames, startHour, dayNumbers.get(currentDayOfWeek), isYudinoActive);
            }
        }

        return items;
    }

    private void addLessonItemsSeparately(List<ScheduleItem> items, String currentDate, String currentDayOfWeek,
                                          String timeRange, String lessonNames, int startHour, Integer dayNumber, boolean isYudinoActive) {
        if (dayNumber == null) dayNumber = 0;

        if (lessonNames.contains(",")) {
            // Разделяем занятия по запятым
            String[] lessons = lessonNames.split(",");
            for (String lesson : lessons) {
                String trimmedLesson = lesson.trim().toLowerCase();
                String normalizedLesson = normalizeLessonName(trimmedLesson);
                if (!normalizedLesson.isEmpty()) {
                    // ДЛЯ КАЖДОГО ЗАНЯТИЯ ВЫЧИСЛЯЕМ isSelected ИНДИВИДУАЛЬНО
                    boolean isSelected = shouldBeSelectedByDefault(startHour, trimmedLesson, dayNumber);
                    items.add(new ScheduleItem(currentDate + " " + currentDayOfWeek, timeRange, normalizedLesson, isSelected, isYudinoActive));
                }
            }
        } else {
            String normalizedLesson = normalizeLessonName(lessonNames.toLowerCase());
            if (!normalizedLesson.isEmpty()) {
                boolean isSelected = shouldBeSelectedByDefault(startHour, lessonNames, dayNumber);
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
        if (lessonName.contains(" ст") || lessonName.startsWith(("ст "))) returnName+=" ст";
        if (lessonName.contains(" мл") || lessonName.contains(("мл "))) returnName+=" мл";

        return returnName;
    }

    private boolean shouldBeSelectedByDefault(int startHour, String lessonName, Integer dayNumber) {
        // Будние дни до 12 - не выделяем
        if (dayNumber != null && dayNumber >= 1 && dayNumber <= 5 && startHour < 12) {
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