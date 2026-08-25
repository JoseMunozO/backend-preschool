package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DiscountDurationTypeConverter implements AttributeConverter<DiscountDurationType, String> {

    @Override
    public String convertToDatabaseColumn(DiscountDurationType type) {
        return type == null ? null : type.getValue();
    }

    @Override
    public DiscountDurationType convertToEntityAttribute(String value) {
        return value == null ? null : DiscountDurationType.fromValue(value);
    }
}
