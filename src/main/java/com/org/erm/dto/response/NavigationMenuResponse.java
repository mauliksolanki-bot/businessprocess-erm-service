package com.org.erm.dto.response;

public record NavigationMenuResponse(
        String code,
        String title,
        String path,
        String icon
) {
}
