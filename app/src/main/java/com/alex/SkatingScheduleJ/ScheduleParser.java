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

        // Извлекаем диапазон дат
        // Возможные форматы:
        // "Расписание 6-10 июля"
        // "Расписание с 1 по 3 июля"
        // "Расписание 01.07-03.07"

        Pattern[] patterns = {
                // 01.07-03.07
                Pattern.compile("(\\d{1,2})\\.(\\d{1,2})-(\\d{1,2})\\.(\\d{1,2})"),

                // 6-10 июля
                Pattern.compile("(\\d{1,2})-(\\d{1,2})\\s+([а-яА-ЯёЁ]+)"),

                // с 1 по 3 июля
                Pattern.compile("с\\s+(\\d{1,2})\\s+по\\s+(\\d{1,2})\\s+([а-яА-ЯёЁ]+)")
        };

        Matcher dateMatcher = null;
        int matchedPattern = -1;

        for (int i = 0; i < patterns.length; i++) {
            dateMatcher = patterns[i].matcher(lines[0]);
            if (dateMatcher.find()) {
                matchedPattern = i;
                break;
            }
        }

        //Берём по умолчанию текущий понедельник
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        int startDay = calendar.get(Calendar.DAY_OF_MONTH);
        int startMonth = calendar.get(Calendar.MONTH) + 1; // Calendar.MONTH возвращает 0-11

        // Если нашли дату — разбираем
        if (matchedPattern != -1) {

            switch (matchedPattern) {

                case 0:
                    // 01.07-03.07
                    startDay = Integer.parseInt(dateMatcher.group(1));
                    startMonth = Integer.parseInt(dateMatcher.group(2));
                    break;

                case 1:
                case 2:
                    // 6-10 июля
                    // с 1 по 3 июля
                    startDay = Integer.parseInt(dateMatcher.group(1));
                    startMonth = monthToNumber(dateMatcher.group(3));
                    break;
            }
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
            java.util.regex.Pattern lessonPattern = java.util.regex.Pattern.compile("^(\\d{1,2}\\.\\d{2})-(\\d{1,2}\\.\\d{2})\\s+(.+)");
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
            String[] lessons = lessonNames.split(",");
            for (String lesson : lessons) {
                String trimmedLesson = lesson.trim().toLowerCase();
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