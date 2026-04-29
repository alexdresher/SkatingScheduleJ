package com.alex.SkatingScheduleJ;

import java.text.SimpleDateFormat;
import java.util.*;

public class ScheduleParser {

    public List<ScheduleItem> parse(String inputText) {
        List<ScheduleItem> items = new ArrayList<>();
        String[] lines = inputText.trim().split("\n");

        // Извлекаем диапазон дат
        java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d{2})\\.(\\d{2})-(\\d{2})\\.(\\d{2})");
        java.util.regex.Matcher dateMatcher = datePattern.matcher(lines[0]);

        int startDay = 27;
        int startMonth = 4;

        if (dateMatcher.find()) {
            startDay = Integer.parseInt(dateMatcher.group(1));
            startMonth = Integer.parseInt(dateMatcher.group(2));
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

            // Проверка на день недели
            java.util.regex.Pattern dayPattern = java.util.regex.Pattern.compile("^(Пн|Вт|Ср|Чт|Пт|Сб|Вс)");
            java.util.regex.Matcher dayMatcher = dayPattern.matcher(trimmed);

            if (dayMatcher.find()) {
                currentDayOfWeek = dayMatcher.group();
                isDayOff = trimmed.contains("Выходной");
                currentDate = calculateDate(startDay, startMonth, currentDayOfWeek, dayOfWeekMap);

                // СБРАСЫВАЕМ Юдино при начале нового дня
                isYudinoActive = false;

                // Проверяем, есть ли слово "Юдино" в строке дня
                // Если да, то включаем Юдино для этой строки и последующих
                if (trimmed.contains("Юдино")) {
                    isYudinoActive = true;
                }

                if (isDayOff) {
                    items.add(new ScheduleItem(currentDate + " " + currentDayOfWeek, "Выходной", "", false));
                }

                // Проверяем занятие в той же строке
                java.util.regex.Pattern timePattern = java.util.regex.Pattern.compile(
                        currentDayOfWeek + "\\s+(\\d{1,2}\\.\\d{2})-(\\d{1,2}\\.\\d{2})\\s+(.+)");
                java.util.regex.Matcher timeMatcher = timePattern.matcher(trimmed);

                if (timeMatcher.find() && !isDayOff) {
                    String startTime = timeMatcher.group(1).replace(".", ":");
                    String endTime = timeMatcher.group(2).replace(".", ":");
                    String timeRange = startTime + " - " + endTime;
                    int startHour = Integer.parseInt(startTime.split(":")[0]);
                    String lessonNames = timeMatcher.group(3);
                    addLessonItemsSeparately(items, currentDate, currentDayOfWeek, timeRange, lessonNames, startHour, dayNumbers.get(currentDayOfWeek), isYudinoActive);
                }
                continue;
            }

            // Проверяем, есть ли Юдино в строке
            boolean lineHasYudino = trimmed.startsWith("Юдино") || trimmed.contains("Юдино");

            if (lineHasYudino) {
                isYudinoActive = true;
                // Удаляем слово "Юдино" из строки
                trimmed = trimmed.replaceFirst("Юдино", "").trim();
            }

            // Обработка строк с занятиями
            java.util.regex.Pattern lessonPattern = java.util.regex.Pattern.compile("^(\\d{1,2}\\.\\d{2})-(\\d{1,2}\\.\\d{2})\\s+(.+)");
            java.util.regex.Matcher lessonMatcher = lessonPattern.matcher(trimmed);

            if (lessonMatcher.find() && !currentDayOfWeek.isEmpty() && !isDayOff) {
                String startTime = lessonMatcher.group(1).replace(".", ":");
                String endTime = lessonMatcher.group(2).replace(".", ":");
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
                    boolean isSelected = shouldBeSelectedByDefault(currentDayOfWeek, startHour, trimmedLesson, dayNumber);
                    items.add(new ScheduleItem(currentDate + " " + currentDayOfWeek, timeRange, normalizedLesson, isSelected, isYudinoActive));
                }
            }
        } else {
            String normalizedLesson = normalizeLessonName(lessonNames.toLowerCase());
            if (!normalizedLesson.isEmpty()) {
                boolean isSelected = shouldBeSelectedByDefault(currentDayOfWeek, startHour, lessonNames, dayNumber);
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
        if (lessonName.contains("лед") || lessonName.contains(("лёд"))) returnName="лёд";
        if (lessonName.contains("раст")) returnName="раст";
        if (lessonName.contains("хор")) returnName="хор";
        if (lessonName.contains("офп")) returnName="офп";
        if (lessonName.contains("сфп")) returnName="сфп";
        if (lessonName.contains(" ст") || lessonName.contains(("ст "))) returnName+=" ст";
        if (lessonName.contains(" мл") || lessonName.contains(("мл "))) returnName+=" мл";


        // Маппинг для стандартных названий
        /*
        Map<String, String> mapping = new HashMap<>();
        mapping.put("лёд", "лёд");
        mapping.put("лед", "лёд");
        mapping.put("офп", "офп");
        mapping.put("сфп", "сфп");
        mapping.put("хор", "хор");
        mapping.put("растяжка", "растяжка");
        mapping.put("раст", "растяжка");
        mapping.put("лёд ст", "лёд ст");
        mapping.put("лед ст", "лёд ст");
        mapping.put("ст лёд", "лёд ст");
        mapping.put("офп ст", "офп ст");
        mapping.put("ст офп", "офп ст");
        mapping.put("сфп ст", "сфп ст");
        mapping.put("ст сфп", "сфп ст");
        mapping.put("хор ст", "хор ст");
        mapping.put("ст хор", "хор ст");
        mapping.put("растяжка ст", "растяжка ст");
        mapping.put("ст раст", "растяжка ст");
        mapping.put("раст ст", "растяжка ст");
        mapping.put("лёд мл", "лёд мл");
        mapping.put("офп мл", "офп мл");
        mapping.put("сфп мл", "сфп мл");
        mapping.put("хор мл", "хор мл");
        mapping.put("растяжка мл", "растяжка мл");
        mapping.put("мл раст", "растяжка мл");
         */

        //return mapping.getOrDefault(name, name);
        return returnName;
    }

    private boolean shouldBeSelectedByDefault(String dayOfWeek, int startHour, String lessonName, Integer dayNumber) {
        // Будние дни до 12 - не выделяем
        if (dayNumber != null && dayNumber >= 1 && dayNumber <= 5 && startHour < 12) {
            return false;
        }

        // Если есть "мл" - не выделяем
        if (lessonName.contains("мл")) {
            return false;
        }

        return true;
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