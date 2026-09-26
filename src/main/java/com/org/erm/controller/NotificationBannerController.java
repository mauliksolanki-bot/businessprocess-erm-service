package com.org.erm.controller;

import com.org.erm.dto.request.NotificationBannerCreateRequest;
import com.org.erm.dto.response.NotificationBannerResponse;
import com.org.erm.service.NotificationBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notification-banners")
@Tag(name = "Notification Banners", description = "Login page notification banner management")
public class NotificationBannerController {

    private final NotificationBannerService bannerService;

    public NotificationBannerController(NotificationBannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping("/active")
    @Operation(summary = "Get notification banners active today")
    public ResponseEntity<List<NotificationBannerResponse>> getActiveBanners() {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(bannerService.getActiveBanners());
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    @Operation(summary = "List notification banners for administrators")
    public ResponseEntity<List<NotificationBannerResponse>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    @Operation(summary = "Create a login notification banner")
    public ResponseEntity<NotificationBannerResponse> createBanner(
            @Valid @RequestBody NotificationBannerCreateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bannerService.createBanner(request, authentication.getName()));
    }
}
