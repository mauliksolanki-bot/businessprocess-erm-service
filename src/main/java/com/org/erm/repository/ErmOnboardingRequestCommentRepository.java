package com.org.erm.repository;

import com.org.erm.model.ErmOnboardingRequestComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmOnboardingRequestCommentRepository extends JpaRepository<ErmOnboardingRequestComment, Long> {

    List<ErmOnboardingRequestComment> findAllByOnboardingRequestIdOrderByActionAtAscIdAsc(Long onboardingRequestId);
}
