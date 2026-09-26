package com.org.erm.service;

import com.org.erm.dto.request.NotificationBannerCreateRequest;
import com.org.erm.dto.response.NotificationBannerResponse;
import com.org.erm.model.ErmNotificationBanner;
import com.org.erm.repository.ErmNotificationBannerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class NotificationBannerService {

    private final ErmNotificationBannerRepository bannerRepository;

    public NotificationBannerService(ErmNotificationBannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationBannerResponse> getActiveBanners() {
        LocalDate currentDate = LocalDate.now();
        return bannerRepository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscIdAsc(
                        currentDate,
                        currentDate
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationBannerResponse> getAllBanners() {
        return bannerRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public NotificationBannerResponse createBanner(NotificationBannerCreateRequest request, String username) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be on or after the start date");
        }

        ErmNotificationBanner banner = new ErmNotificationBanner();
        banner.setTitle(request.title().trim());
        banner.setMessage(request.message().trim());
        banner.setStartDate(request.startDate());
        banner.setEndDate(request.endDate());
        banner.setNotificationType(request.notificationType());
        banner.setCreatedByUsername(username);
        return toResponse(bannerRepository.save(banner));
    }

    private NotificationBannerResponse toResponse(ErmNotificationBanner banner) {
        return new NotificationBannerResponse(
                banner.getId(),
                banner.getTitle(),
                banner.getMessage(),
                banner.getStartDate(),
                banner.getEndDate(),
                banner.getNotificationType(),
                banner.getCreatedByUsername(),
                banner.getCreatedAt()
        );
    }
}
