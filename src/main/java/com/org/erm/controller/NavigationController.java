package com.org.erm.controller;

import com.org.erm.dto.NavigationMenuResponse;
import com.org.erm.service.NavigationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/navigation")
public class NavigationController {

    private final NavigationService navigationService;

    public NavigationController(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    @GetMapping("/menus")
    public ResponseEntity<List<NavigationMenuResponse>> getAuthorizedMenus(Authentication authentication) {
        return ResponseEntity.ok(navigationService.getAuthorizedMenus(authentication.getName()));
    }
}
