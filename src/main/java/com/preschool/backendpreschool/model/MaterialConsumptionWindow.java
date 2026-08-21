package com.preschool.backendpreschool.model;

public enum MaterialConsumptionWindow {
    WEEK(7),
    MONTH(30),
    THREE_MONTHS(90),
    SIX_MONTHS(180),
    TWELVE_MONTHS(365);

    private final int days;

    MaterialConsumptionWindow(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }
}
