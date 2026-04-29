package com.alex.SkatingScheduleJ;

import android.Manifest;
import android.accounts.Account;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ScheduleTableActivityG extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ScheduleAdapter adapter;
    private List<ScheduleItem> items = new ArrayList<>();

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final int REQUEST_AUTHORIZATION = 101;
    private static final int MAX_RETRIES = 1;  // 3 попытки
    private static final String TARGET_ACCOUNT_NAME = "advnoob@gmail.com";

    private List<ScheduleItem> pendingItems = null;

    private final List<String> standardLessons = Arrays.asList(
            "", "лёд", "лёд ст", "лёд мл", "офп", "офп ст", "офп мл",
            "сфп", "сфп ст", "сфп мл", "хор", "хор ст", "хор мл",
            "растяжка", "растяжка ст", "растяжка мл"
    );

    private final String[] requiredPermissions = {
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_table);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        items = (List<ScheduleItem>) getIntent().getSerializableExtra("schedule_items");
        if (items == null) items = new ArrayList<>();

        adapter = new ScheduleAdapter(items);
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
        pendingItems = selectedItems;

        List<String> missingPermissions = new ArrayList<>();
        for (String perm : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(perm);
            }
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        } else {
            createEventsViaCalendarApi(selectedItems);
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
            if (allGranted && pendingItems != null) {
                createEventsViaCalendarApi(pendingItems);
            } else if (!allGranted) {
                Toast.makeText(this, "Нужны разрешения для работы с календарём", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_AUTHORIZATION) {
            if (resultCode == RESULT_OK && pendingItems != null) {
                Toast.makeText(this, "Разрешение получено. Повторяем добавление...", Toast.LENGTH_SHORT).show();
                createEventsViaCalendarApi(pendingItems);
            } else {
                Toast.makeText(this, "Разрешение не получено.", Toast.LENGTH_LONG).show();
                pendingItems = null;
            }
        }
    }

    private GoogleAccountCredential getCredential() {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this,
                Collections.singletonList(CalendarScopes.CALENDAR)
        );
        credential.setSelectedAccount(new Account(TARGET_ACCOUNT_NAME, "com.google"));
        return credential;
    }

    private Calendar getCalendarService() {
        GoogleAccountCredential credential = getCredential();

        return new Calendar.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        )
                .setApplicationName("SkatingScheduleJ")
                .build();
    }

    private void createEventsViaCalendarApi(List<ScheduleItem> selectedItems) {
        pendingItems = selectedItems;

        Executors.newSingleThreadExecutor().execute(() -> {
            int successCount = 0;
            int failCount = 0;
            StringBuilder errors = new StringBuilder();

            try {
                Calendar service = getCalendarService();
                String calendarId = "advnoob@gmail.com";

                for (ScheduleItem item : selectedItems) {
                    String dateStr = item.getDayAndNumber().split(" ")[0];
                    String[] timeParts = parseTimeRange(item.getTimeRange());
                    String startTime = timeParts[0];
                    String endTime = timeParts[1];

                    String startDateTimeStr = formatDateTimeForApi(dateStr, startTime);
                    String endDateTimeStr = formatDateTimeForApi(dateStr, endTime);

                    Event event = new Event()
                            .setSummary(item.getSelectedLesson())
                            .setDescription(item.getDayAndNumber() + " " + item.getTimeRange());

                    com.google.api.client.util.DateTime startDateTime =
                            new com.google.api.client.util.DateTime(startDateTimeStr);
                    EventDateTime start = new EventDateTime()
                            .setDateTime(startDateTime)
                            .setTimeZone("Europe/Moscow");
                    event.setStart(start);

                    com.google.api.client.util.DateTime endDateTime =
                            new com.google.api.client.util.DateTime(endDateTimeStr);
                    EventDateTime end = new EventDateTime()
                            .setDateTime(endDateTime)
                            .setTimeZone("Europe/Moscow");
                    event.setEnd(end);

                    boolean added = false;
                    String lastError = "";

                    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                        try {
                            Event createdEvent = service.events().insert(calendarId, event).execute();
                            if (createdEvent != null && createdEvent.getId() != null) {
                                added = true;
                                break;
                            }
                        } catch (UserRecoverableAuthIOException e) {
                            lastError = "Ошибка авторизации";
                            runOnUiThread(() -> {
                                try {
                                    Intent recoverIntent = e.getIntent();
                                    startActivityForResult(recoverIntent, REQUEST_AUTHORIZATION);
                                } catch (Exception ex) {
                                    Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
                                }
                            });
                            return;
                        } catch (Exception e) {
                            lastError = e.getMessage();
                            if (attempt < MAX_RETRIES) {
                                /*try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                }*/
                            }
                        }
                    }

                    if (added) {
                        successCount++;
                    } else {
                        failCount++;
                        errors.append(item.getSelectedLesson()).append(": ").append(lastError).append("\n");
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
                    pendingItems = null;
                });

            } catch (Exception e) {
                final String error = e.getMessage();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Ошибка: " + error, Toast.LENGTH_LONG).show();
                    pendingItems = null;
                });
            }
        });
    }

    private String formatDateTimeForApi(String dateStr, String timeStr) {
        try {
            String[] parts = dateStr.split("\\.");
            String day = String.format("%02d", Integer.parseInt(parts[0]));
            String month = String.format("%02d", Integer.parseInt(parts[1]));

            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

            return String.format("%04d-%s-%sT%s:00+03:00", currentYear, month, day, timeStr);
        } catch (Exception e) {
            return "";
        }
    }

    private String[] parseTimeRange(String timeRange) {
        return timeRange.split(" - ");
    }

    // Адаптер для RecyclerView
    private class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {
        private List<ScheduleItem> items;

        ScheduleAdapter(List<ScheduleItem> items) {
            this.items = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
            private View daySeparator;
            private TextView dateText;
            private TextView timeText;
            private Spinner lessonSpinner;
            private CheckBox importCheckbox;

            ViewHolder(View itemView) {
                super(itemView);
                daySeparator = itemView.findViewById(R.id.daySeparator);
                dateText = itemView.findViewById(R.id.dateText);
                timeText = itemView.findViewById(R.id.timeText);
                lessonSpinner = itemView.findViewById(R.id.lessonSpinner);
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

                dateText.setText(item.getDayAndNumber());
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

                importCheckbox.setOnCheckedChangeListener(null);
                importCheckbox.setChecked(item.isSelected());
                importCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    item.setSelected(isChecked);
                });

                if (item.getTimeRange().equals("Выходной")) {
                    importCheckbox.setEnabled(false);
                    lessonSpinner.setEnabled(false);
                } else {
                    importCheckbox.setEnabled(true);
                    lessonSpinner.setEnabled(true);
                }
            }
        }
    }
}