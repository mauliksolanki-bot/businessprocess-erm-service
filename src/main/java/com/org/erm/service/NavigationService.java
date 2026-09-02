package com.org.erm.service;

import com.org.erm.dto.NavigationMenuResponse;
import com.org.erm.model.ErmNavMenu;
import com.org.erm.repository.ErmNavMenuRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NavigationService {

    private final ErmNavMenuRepository ermNavMenuRepository;

    public NavigationService(ErmNavMenuRepository ermNavMenuRepository) {
        this.ermNavMenuRepository = ermNavMenuRepository;
    }

    public List<NavigationMenuResponse> getAuthorizedMenus(String username) {
        return ermNavMenuRepository.findAuthorizedMenusByUsername(username).stream()
                .map(this::toResponse)
                .toList();
    }

    private NavigationMenuResponse toResponse(ErmNavMenu menu) {
        return new NavigationMenuResponse(
                menu.getMenuCode(),
                menu.getMenuTitle(),
                menu.getMenuPath(),
                menu.getMenuIcon()
        );
    }
}
