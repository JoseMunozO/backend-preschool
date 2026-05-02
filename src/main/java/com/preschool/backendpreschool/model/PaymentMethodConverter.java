package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethod method) {
        return method == null ? null : method.getValue();
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String value) {
        return value == null ? null : PaymentMethod.fromValue(value);
    }
}
