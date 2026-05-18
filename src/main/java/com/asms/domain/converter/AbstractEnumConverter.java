package com.asms.domain.converter;

import jakarta.persistence.AttributeConverter;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractEnumConverter<E extends Enum<E> & ConvertableEnum>
        implements AttributeConverter<E, Integer> {

    private final Map<Integer, E> map = new HashMap<>();
    private final Class<E> enumClass;

    protected AbstractEnumConverter(Class<E> enumClass) {
        this.enumClass = enumClass;
        for (E value : enumClass.getEnumConstants()) {
            if (map.containsKey(value.getKey())) {
                throw new IllegalStateException(
                        enumClass.getSimpleName() + " has duplicate key: " + value.getKey());
            }
            map.put(value.getKey(), value);
        }
    }

    @Override
    public Integer convertToDatabaseColumn(E attribute) {
        return attribute != null ? attribute.getKey() : null;
    }

    @Override
    public E convertToEntityAttribute(Integer dbData) {
        if (dbData == null) return null;
        E result = map.get(dbData);
        if (result == null) {
            throw new IllegalArgumentException(
                    "Unknown " + enumClass.getSimpleName() + " key: " + dbData);
        }
        return result;
    }
}
