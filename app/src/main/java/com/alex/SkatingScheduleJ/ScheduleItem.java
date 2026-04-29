package com.alex.SkatingScheduleJ;

import java.io.Serializable;

public class ScheduleItem implements Serializable {
    private String dayAndNumber;
    private String timeRange;
    private String selectedLesson;
    private boolean isSelected;
    private boolean isYudino;  // Новый признак

    public ScheduleItem(String dayAndNumber, String timeRange, String selectedLesson, boolean isSelected) {
        this.dayAndNumber = dayAndNumber;
        this.timeRange = timeRange;
        this.selectedLesson = selectedLesson;
        this.isSelected = isSelected;
        this.isYudino = false;  // По умолчанию false
    }

    public ScheduleItem(String dayAndNumber, String timeRange, String selectedLesson, boolean isSelected, boolean isYudino) {
        this.dayAndNumber = dayAndNumber;
        this.timeRange = timeRange;
        this.selectedLesson = selectedLesson;
        this.isSelected = isSelected;
        this.isYudino = isYudino;
    }

    public String getDayAndNumber() { return dayAndNumber; }
    public void setDayAndNumber(String dayAndNumber) { this.dayAndNumber = dayAndNumber; }

    public String getTimeRange() { return timeRange; }
    public void setTimeRange(String timeRange) { this.timeRange = timeRange; }

    public String getSelectedLesson() { return selectedLesson; }
    public void setSelectedLesson(String selectedLesson) { this.selectedLesson = selectedLesson; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    public boolean isYudino() { return isYudino; }
    public void setYudino(boolean yudino) { isYudino = yudino; }
}