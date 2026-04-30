package com.preschool.backendpreschool.model;

public enum ParentStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    ParentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ParentStatus fromValue(String value) {
        for (ParentStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de padre/tutor no valido: " + value);
    }
}
