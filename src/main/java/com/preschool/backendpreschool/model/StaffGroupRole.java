package com.preschool.backendpreschool.model;

public enum StaffGroupRole {
    TEACHER("teacher"),
    ASSISTANT("assistant"),
    COORDINATOR("coordinator");

    private final String value;

    StaffGroupRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static StaffGroupRole fromValue(String value) {
        for (StaffGroupRole role : values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rol de personal en grupo no valido: " + value);
    }
}
