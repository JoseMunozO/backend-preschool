package com.preschool.backendpreschool.model;

public enum DiscountType {
    PERCENTAGE("percentage"),
    FIXED_AMOUNT("fixed_amount");

    private final String value;

    DiscountType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DiscountType fromValue(String value) {
        for (DiscountType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de descuento no valido: " + value);
    }
}
