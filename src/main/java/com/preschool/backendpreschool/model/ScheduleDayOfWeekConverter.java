package com.preschool.backendpreschool.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;

@Converter(autoApply = false)
public class ScheduleDayOfWeekConverter implements AttributeConverter<DayOfWeek, Byte> {

    @Override
    public Byte convertToDatabaseColumn(DayOfWeek dayOfWeek) {
        return dayOfWeek == null ? null : (byte) dayOfWeek.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Byte value) {
        if (value == null) {
            return null;
        }
        if (value < 1 || value > 7) {
            throw new IllegalArgumentException("Dia de semana no valido: " + value);
        }
        return DayOfWeek.of(value);
    }
}
