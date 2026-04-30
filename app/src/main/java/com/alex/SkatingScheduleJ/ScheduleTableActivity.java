package com.alex.SkatingScheduleJ;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;

public class ScheduleTableActivity extends AppCompatActivity {

    private List<ScheduleItem> items = new ArrayList<>();

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String TARGET_ACCOUNT_NAME = "advnoob@gmail.com";

    // Список всех возможных занятий для удаления
    private final List<String> ALL_LESSONS = Arrays.asList(
            "Лёд", "Лёд ст", "Лёд мл",
            "ОФП", "ОФП ст", "ОФП мл",
            "СФП", "СФП ст", "СФП мл",
            "Хор", "Хор ст", "Хор мл",
            "Раст", "Раст ст", "Раст мл"
    );

    private final List<String> standardLessons;

    {
        standardLessons = new ArrayList<>();
        standardLessons.add("");  // Пустая строка для выходных
        standardLessons.addAll(ALL_LESSONS);
    }

    private final String[] requiredPermissions = {
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_table);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        items = (List<ScheduleItem>) getIntent().getSerializableExtra("schedule_items");
        if (items == null) items = new ArrayList<>();

        ScheduleAdapter adapter = new ScheduleAdapter(items);
        recyclerView.setAdapter(adapter);

        Button addToCalendarButton = findViewById(R.id.addToCalendarButton);
        addToCalendarButton.setOnClickListener(v -> {
            List<ScheduleItem> selectedItems = new ArrayList<>();
            for (ScheduleItem item : items) {
                if (item.isSelected() && !item.getSelectedLesson().isEmpty()) {
                    selectedItems.add(item);
                }
            }
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Выберите хотя бы одно занятие", Toast.LENGTH_SHORT).show();
            } else {
                checkPermissionsAndAdd(selectedItems);
            }
        });
    }

    private void checkPermissionsAndAdd(List<ScheduleItem> selectedItems) {
        List<String> missingPermissions = new ArrayList<>();
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(perm);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        } else {
            addEventsToCalendar(selectedItems);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                Toast.makeText(this, "Разрешения получены", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Нужны разрешения для работы с календарём", Toast.LENGTH_LONG).show();
            }
        }
    }

    // ==================== ДОБАВЛЕНИЕ В КАЛЕНДАРЬ ====================

    private long getCalendarId() {
        String[] projection = {
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        };

        String selection = CalendarContract.Calendars.ACCOUNT_NAME + " = ?";
        String[] selectionArgs = {TARGET_ACCOUNT_NAME};

        Cursor cursor = getContentResolver().query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        while (cursor != null && cursor.moveToNext()) {
            String calendarName=cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME));
            if (calendarName.equals("advnoob@gmail.com")) {
                long calendarId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID));
                cursor.close();
                return calendarId;
            }
        }

        if (cursor != null) cursor.close();
        return -1;
    }

    /**
     * Удаляет ВСЕ занятия из списка ALL_LESSONS за указанную дату
     */
    private void deleteAllEventsForDate(String dateStr, long calendarId) {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

        try {
            Date date = dateFormat.parse(dateStr + "." + currentYear);
            if (date == null) return;

            long startOfDay = date.getTime();
            long endOfDay = startOfDay + (24 * 60 * 60 * 1000);

            //добавляем Юдино
            List<String> All_lessons_with_Udino = new ArrayList<>(ALL_LESSONS);
            for (String lesson : ALL_LESSONS)
            {
                All_lessons_with_Udino.add(lesson + " Юдино");
            }

            // Собираем все названия занятий для удаления
            StringBuilder titles = new StringBuilder();
            for (int i = 0; i < All_lessons_with_Udino.size(); i++) {
                if (i > 0) titles.append(" OR ");
                titles.append(CalendarContract.Events.TITLE).append(" = ?");
            }

            String selection = CalendarContract.Events.CALENDAR_ID + " = ? AND " +
                    "(" + titles + ") AND " +
                    CalendarContract.Events.DTSTART + " >= ? AND " +
                    CalendarContract.Events.DTSTART + " < ?";

            List<String> args = new ArrayList<>();
            args.add(String.valueOf(calendarId));
            args.addAll(All_lessons_with_Udino);
            args.add(String.valueOf(startOfDay));
            args.add(String.valueOf(endOfDay));

            String[] selectionArgs = args.toArray(new String[0]);

            int deletedCount = getContentResolver().delete(CalendarContract.Events.CONTENT_URI, selection, selectionArgs);

            if (deletedCount > 0) {
                android.util.Log.d("CalendarDebug", "Удалено " + deletedCount + " событий за " + dateStr);
            }

        } catch (Exception e) {
            android.util.Log.e("CalendarDebug", "Ошибка удаления: " + e.getMessage());
        }
    }

    private void addEventsToCalendar(List<ScheduleItem> selectedItems) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int successCount = 0;
            int failCount = 0;
            StringBuilder errors = new StringBuilder();

            long calendarId = getCalendarId();
            if (calendarId == -1) {
                runOnUiThread(() -> Toast.makeText(this, "Календарь для " + TARGET_ACCOUNT_NAME + " не найден", Toast.LENGTH_LONG).show());
                return;
            }

            // Группируем события по датам, чтобы удалить все занятия за день один раз
            Map<String, List<ScheduleItem>> itemsByDate = new HashMap<>();
            for (ScheduleItem item : selectedItems) {
                String dateStr = item.getDayAndNumber().split(" ")[0];
                if (!itemsByDate.containsKey(dateStr)) {
                    itemsByDate.put(dateStr, new ArrayList<>());
                }
                Objects.requireNonNull(itemsByDate.get(dateStr)).add(item);
            }

            // Для каждой даты: сначала удаляем ВСЕ занятия, потом добавляем выбранные
            for (Map.Entry<String, List<ScheduleItem>> entry : itemsByDate.entrySet()) {
                String dateStr = entry.getKey();
                List<ScheduleItem> dateItems = entry.getValue();

                // 1. Удаляем ВСЕ возможные занятия за эту дату
                deleteAllEventsForDate(dateStr, calendarId);

                // 2. Добавляем выбранные занятия
                for (ScheduleItem item : dateItems) {
                    try {
                        String[] timeParts = item.getTimeRange().split(" - ");
                        String startTime = timeParts[0];
                        String endTime = timeParts[1];

                        long startMillis = getDateTimeMillis(dateStr, startTime);
                        long endMillis = getDateTimeMillis(dateStr, endTime);

                        if (startMillis == -1 || endMillis == -1) {
                            failCount++;
                            errors.append(item.getSelectedLesson()).append(": ошибка парсинга времени\n");
                            continue;
                        }

                        ContentValues values = new ContentValues();
                        values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
                        values.put(CalendarContract.Events.TITLE, item.getSelectedLesson()+(item.isYudino()?" Юдино":""));
                        //values.put(CalendarContract.Events.DESCRIPTION, item.getDayAndNumber() + " " + item.getTimeRange());
                        values.put(CalendarContract.Events.DTSTART, startMillis);
                        values.put(CalendarContract.Events.DTEND, endMillis);
                        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
                        values.put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
                        values.put("eventStatus", 1);
                        values.put(CalendarContract.Events.HAS_ALARM, 0);

                        String eventUriString = "content://com.android.calendar/events";
                        Uri uri = getContentResolver().insert(Uri.parse(eventUriString), values);

                        if (null != uri) successCount++;
                        else {
                            failCount++;
                            errors.append(item.getSelectedLesson()).append(": ошибка добавления\n");
                        }

                    } catch (Exception e) {
                        failCount++;
                        errors.append(item.getSelectedLesson()).append(": ").append(e.getMessage()).append("\n");
                    }
                }
            }

            final int finalSuccess = successCount;
            final int finalFail = failCount;
            final String finalErrors = errors.toString();

            runOnUiThread(() -> {
                if (finalFail == 0) {
                    Toast.makeText(this, finalSuccess + " занятий добавлено в календарь", Toast.LENGTH_LONG).show();
                } else {
                    String message = finalSuccess + " добавлено, " + finalFail + " ошибок.\n" + finalErrors;
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private long getDateTimeMillis(String dateStr, String timeStr) {
        try {
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            String dateTimeStr = dateStr + "." + currentYear + " " + timeStr;
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            Date date = format.parse(dateTimeStr);
            return date != null ? date.getTime() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    // ==================== АДАПТЕР ====================

    private class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {
        private List<ScheduleItem> items;

        ScheduleAdapter(List<ScheduleItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_schedule_row, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(items.get(position), position);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final View daySeparator;
            private final TextView dateText;
            private final TextView dayOfWeekText;
            private final TextView timeText;
            private final Spinner lessonSpinner;
            private final CheckBox yudinoCheckbox;
            private final CheckBox importCheckbox;

            ViewHolder(View itemView) {
                super(itemView);
                daySeparator = itemView.findViewById(R.id.daySeparator);
                dateText = itemView.findViewById(R.id.dateText);
                dayOfWeekText = itemView.findViewById(R.id.dayOfWeekText);
                timeText = itemView.findViewById(R.id.timeText);
                lessonSpinner = itemView.findViewById(R.id.lessonSpinner);
                yudinoCheckbox = itemView.findViewById(R.id.yudinoCheckbox);
                importCheckbox = itemView.findViewById(R.id.importCheckbox);
            }

            void bind(ScheduleItem item, int position) {
                if (position > 0) {
                    String prevDay = items.get(position - 1).getDayAndNumber().split(" ")[0];
                    String currentDay = item.getDayAndNumber().split(" ")[0];
                    daySeparator.setVisibility(prevDay.equals(currentDay) ? View.GONE : View.VISIBLE);
                } else {
                    daySeparator.setVisibility(View.GONE);
                }

                // Разделяем дату и день недели
                String[] parts = item.getDayAndNumber().split(" ");
                String dateStr = parts[0];
                String dayOfWeekStr = parts[1];

                // Устанавливаем текст
                dateText.setText(dateStr);
                dayOfWeekText.setText(dayOfWeekStr);
                timeText.setText(item.getTimeRange());

                ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(
                        itemView.getContext(),
                        android.R.layout.simple_spinner_item,
                        standardLessons
                );
                adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                lessonSpinner.setAdapter(adapterSpinner);

                int selectedIndex = standardLessons.indexOf(item.getSelectedLesson());
                if (selectedIndex >= 0) {
                    lessonSpinner.setSelection(selectedIndex);
                }

                lessonSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        item.setSelectedLesson(standardLessons.get(pos));
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });

                // Чекбокс "Юдино"
                yudinoCheckbox.setOnCheckedChangeListener(null);
                yudinoCheckbox.setChecked(item.isYudino());
                yudinoCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> item.setYudino(isChecked));

                importCheckbox.setOnCheckedChangeListener(null);
                importCheckbox.setChecked(item.isSelected());
                importCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> item.setSelected(isChecked));

                if (item.getTimeRange().equals("Выходной")) {
                    importCheckbox.setEnabled(false);
                    lessonSpinner.setEnabled(false);
                    yudinoCheckbox.setEnabled(false);
                } else {
                    importCheckbox.setEnabled(true);
                    lessonSpinner.setEnabled(true);
                    yudinoCheckbox.setEnabled(true);
                }
            }
        }
    }
}