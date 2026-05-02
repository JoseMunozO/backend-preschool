package com.preschool.backendpreschool.model;

public enum StudentChargeStatus {
    PENDING("pending"),
    PARTIALLY_PAID("partially_paid"),
    PAID("paid"),
    CANCELLED("cancelled"),
    OVERDUE("overdue");

    private final String value;

    StudentChargeStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StudentChargeStatus fromValue(String value) {
        for (StudentChargeStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de cargo no valido: " + value);
    }
}
