package com.org.erm.repository;

import com.org.erm.model.ErmTokenBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ErmTokenBlacklistRepository extends JpaRepository<ErmTokenBlacklist, Long> {

    boolean existsByTokenHashAndExpiresAtAfter(String tokenHash, LocalDateTime now);

    boolean existsByTokenHash(String tokenHash);
}
