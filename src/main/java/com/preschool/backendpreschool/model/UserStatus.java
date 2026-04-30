package com.preschool.backendpreschool.model;

public enum UserStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    LOCKED("locked"),
    PENDING_ACTIVATION("pending_activation");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserStatus fromValue(String value) {
        for (UserStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Estado de usuario no valido: " + value);
    }
}
