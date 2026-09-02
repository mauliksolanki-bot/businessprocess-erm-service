package com.org.erm.dto;

public record NavigationMenuResponse(
        String code,
        String title,
        String path,
        String icon
) {
}
