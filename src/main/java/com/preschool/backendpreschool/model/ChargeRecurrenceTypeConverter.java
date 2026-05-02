package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ChargeRecurrenceTypeConverter implements AttributeConverter<ChargeRecurrenceType, String> {

    @Override
    public String convertToDatabaseColumn(ChargeRecurrenceType type) {
        return type == null ? null : type.getValue();
    }

    @Override
    public ChargeRecurrenceType convertToEntityAttribute(String value) {
        return value == null ? null : ChargeRecurrenceType.fromValue(value);
    }
}
