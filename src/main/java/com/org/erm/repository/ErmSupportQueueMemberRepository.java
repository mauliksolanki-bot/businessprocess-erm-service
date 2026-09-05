package com.org.erm.repository;

import com.org.erm.model.ErmSupportQueueMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErmSupportQueueMemberRepository extends JpaRepository<ErmSupportQueueMember, Long> {

    List<ErmSupportQueueMember> findAllByQueueIdAndActiveTrueOrderByLastAssignedAtAscIdAsc(Long queueId);

    Optional<ErmSupportQueueMember> findByQueueIdAndUserId(Long queueId, Long userId);

    Optional<ErmSupportQueueMember> findByQueueIdAndUserIdAndActiveTrue(Long queueId, Long userId);

    boolean existsByQueueIdAndUserIdAndActiveTrue(Long queueId, Long userId);

    boolean existsByUserIdAndActiveTrue(Long userId);

    List<ErmSupportQueueMember> findAllByUserIdAndActiveTrue(Long userId);

    long countByQueueIdAndActiveTrue(Long queueId);
}
