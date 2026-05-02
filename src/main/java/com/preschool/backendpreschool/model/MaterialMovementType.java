package com.preschool.backendpreschool.model;

public enum MaterialMovementType {
    IN("in"),
    OUT("out"),
    ADJUSTMENT("adjustment");

    private final String value;

    MaterialMovementType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static MaterialMovementType fromValue(String value) {
        for (MaterialMovementType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de movimiento no valido: " + value);
    }
}
