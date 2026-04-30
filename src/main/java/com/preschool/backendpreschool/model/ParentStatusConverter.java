package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ParentStatusConverter implements AttributeConverter<ParentStatus, String> {

    @Override
    public String convertToDatabaseColumn(ParentStatus status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public ParentStatus convertToEntityAttribute(String value) {
        return value == null ? null : ParentStatus.fromValue(value);
    }
}
