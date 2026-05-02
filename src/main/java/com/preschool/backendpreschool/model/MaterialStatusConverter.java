package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class MaterialStatusConverter implements AttributeConverter<MaterialStatus, String> {

    @Override
    public String convertToDatabaseColumn(MaterialStatus status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public MaterialStatus convertToEntityAttribute(String value) {
        return value == null ? null : MaterialStatus.fromValue(value);
    }
}
