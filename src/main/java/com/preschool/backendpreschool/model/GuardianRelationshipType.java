package com.preschool.backendpreschool.model;

public enum GuardianRelationshipType {
    FATHER("father"),
    MOTHER("mother"),
    GUARDIAN("guardian"),
    RELATIVE("relative"),
    OTHER("other");

    private final String value;

    GuardianRelationshipType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static GuardianRelationshipType fromValue(String value) {
        for (GuardianRelationshipType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Relacion de tutor no valida: " + value);
    }
}
