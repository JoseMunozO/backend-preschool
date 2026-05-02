package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StudentChargeStatusConverter implements AttributeConverter<StudentChargeStatus, String> {

    @Override
    public String convertToDatabaseColumn(StudentChargeStatus status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public StudentChargeStatus convertToEntityAttribute(String value) {
        return value == null ? null : StudentChargeStatus.fromValue(value);
    }
}
