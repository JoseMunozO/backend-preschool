package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DiscountTypeConverter implements AttributeConverter<DiscountType, String> {

    @Override
    public String convertToDatabaseColumn(DiscountType type) {
        return type == null ? null : type.getValue();
    }

    @Override
    public DiscountType convertToEntityAttribute(String value) {
        return value == null ? null : DiscountType.fromValue(value);
    }
}
