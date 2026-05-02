package com.preschool.backendpreschool.model;

public enum MaterialStatus {
    ACTIVE("active"),
    ARCHIVED("archived");

    private final String value;

    MaterialStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MaterialStatus fromValue(String value) {
        for (MaterialStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de material no valido: " + value);
    }
}
