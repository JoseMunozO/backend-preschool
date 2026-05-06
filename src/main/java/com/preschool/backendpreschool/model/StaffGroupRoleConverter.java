package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class StaffGroupRoleConverter implements AttributeConverter<StaffGroupRole, String> {

    @Override
    public String convertToDatabaseColumn(StaffGroupRole role) {
        return role == null ? null : role.getValue();
    }

    @Override
    public StaffGroupRole convertToEntityAttribute(String value) {
        return value == null ? null : StaffGroupRole.fromValue(value);
    }
}
