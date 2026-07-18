package com.alex.SkatingScheduleJ;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText inputText;
    private Button processButton;

    Switch afterTimeSwitch;
    EditText afterTimeInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        afterTimeSwitch = findViewById(R.id.afterTimeSwitch);
        afterTimeInput = findViewById(R.id.afterTimeInput);


        inputText = findViewById(R.id.inputText);
        processButton = findViewById(R.id.processButton);

        processButton.setOnClickListener(v -> {
            String text = inputText.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "Вставьте текст расписания", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean afterTimeEnabled = afterTimeSwitch.isChecked();
            int afterHour = 12;

            if (afterTimeEnabled) {
                String timeText = afterTimeInput.getText().toString().trim();
                try {
                    afterHour = Integer.parseInt(timeText);
                } catch (Exception e) {
                    // если формат некорректный, останется значение по умолчанию (12)
                }
            }

            ScheduleParser parser = new ScheduleParser();
            java.util.List<ScheduleItem> scheduleItems = parser.parse(text,afterTimeEnabled, afterHour);

            if (scheduleItems.isEmpty()) {
                Toast.makeText(this, "Не удалось распознать расписание", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, ScheduleTableActivity.class);
            intent.putExtra("schedule_items", new java.util.ArrayList<>(scheduleItems));
            startActivity(intent);
        });
    }
}