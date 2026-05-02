package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class MaterialMovementTypeConverter implements AttributeConverter<MaterialMovementType, String> {

    @Override
    public String convertToDatabaseColumn(MaterialMovementType type) {
        return type == null ? null : type.getValue();
    }

    @Override
    public MaterialMovementType convertToEntityAttribute(String value) {
        return value == null ? null : MaterialMovementType.fromValue(value);
    }
}
