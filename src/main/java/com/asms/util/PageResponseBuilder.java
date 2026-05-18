package com.asms.util;

import com.asms.model.PagedResponseDto;
import org.springframework.data.domain.Page;

import java.util.List;

public final class PageResponseBuilder {

    private PageResponseBuilder() {}

    public static PagedResponseDto build(List<?> content, Page<?> page) {
        return build(content, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    public static PagedResponseDto build(List<?> content, long totalElements, int page, int size) {
        PagedResponseDto dto = new PagedResponseDto();
        dto.setContent(content.stream().map(i -> (Object) i).toList());
        dto.setTotalElements(totalElements);
        dto.setNumber(page);
        dto.setSize(size);
        dto.setTotalPages(size > 0 ? (int) Math.ceil((double) totalElements / size) : 0);
        return dto;
    }
}
