package com.preschool.backendpreschool.model;

public enum ChargeRecurrenceType {
    ONE_TIME("one_time"),
    MONTHLY("monthly"),
    CUSTOM("custom");

    private final String value;

    ChargeRecurrenceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ChargeRecurrenceType fromValue(String value) {
        for (ChargeRecurrenceType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Recurrencia de cargo no valida: " + value);
    }
}
