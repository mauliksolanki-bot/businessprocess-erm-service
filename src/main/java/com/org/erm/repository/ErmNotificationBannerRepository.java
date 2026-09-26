package com.org.erm.repository;

import com.org.erm.model.ErmNotificationBanner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ErmNotificationBannerRepository extends JpaRepository<ErmNotificationBanner, Long> {

    List<ErmNotificationBanner> findAllByOrderByCreatedAtDesc();

    List<ErmNotificationBanner> findAllByActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscIdAsc(
            LocalDate currentDate,
            LocalDate currentDateForEnd
    );
}
