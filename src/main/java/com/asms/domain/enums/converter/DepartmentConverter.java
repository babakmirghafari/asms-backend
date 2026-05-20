package com.asms.domain.enums.converter;

import com.asms.domain.converter.AbstractEnumConverter;
import com.asms.domain.enums.Department;
import com.asms.domain.enums.UserStatus;
import jakarta.persistence.Converter;

@Converter
public class DepartmentConverter extends AbstractEnumConverter<Department> {
    public DepartmentConverter() {
        super(Department.class);
    }
}
