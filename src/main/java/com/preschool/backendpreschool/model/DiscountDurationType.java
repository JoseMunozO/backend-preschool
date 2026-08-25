package com.preschool.backendpreschool.model;

public enum DiscountDurationType {
    INSTANT("instant"),
    SCHEDULED("scheduled");

    private final String value;

    DiscountDurationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DiscountDurationType fromValue(String value) {
        for (DiscountDurationType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de duracion de descuento no valido: " + value);
    }
}
