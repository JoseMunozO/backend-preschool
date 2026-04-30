package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GuardianRelationshipTypeConverter implements AttributeConverter<GuardianRelationshipType, String> {

    @Override
    public String convertToDatabaseColumn(GuardianRelationshipType relationshipType) {
        return relationshipType == null ? null : relationshipType.getValue();
    }

    @Override
    public GuardianRelationshipType convertToEntityAttribute(String value) {
        return value == null ? null : GuardianRelationshipType.fromValue(value);
    }
}
