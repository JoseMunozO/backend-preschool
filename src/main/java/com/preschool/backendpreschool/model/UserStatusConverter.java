package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {

    @Override
    public String convertToDatabaseColumn(UserStatus status) {
        return status == null ? null : status.getValue();
    }

    @Override
    public UserStatus convertToEntityAttribute(String value) {
        return value == null ? null : UserStatus.fromValue(value);
    }
}
