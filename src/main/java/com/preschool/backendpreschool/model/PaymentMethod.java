package com.preschool.backendpreschool.model;

public enum PaymentMethod {
    CASH("cash"),
    CARD("card"),
    TRANSFER("bank_transfer"),
    SWISH("swish"),
    OTHER("other");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PaymentMethod fromValue(String value) {
        for (PaymentMethod method : values()) {
            if (method.value.equalsIgnoreCase(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Metodo de pago no valido: " + value);
    }
}
