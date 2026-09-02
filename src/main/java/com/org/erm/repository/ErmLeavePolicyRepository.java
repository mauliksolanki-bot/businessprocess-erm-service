package com.org.erm.repository;

import com.org.erm.model.ErmLeavePolicy;
import com.org.erm.model.LeaveCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErmLeavePolicyRepository extends JpaRepository<ErmLeavePolicy, Long> {
    Optional<ErmLeavePolicy> findByLeaveCategory(LeaveCategory leaveCategory);
    boolean existsByLeaveCategory(LeaveCategory leaveCategory);
    List<ErmLeavePolicy> findAllByOrderByLeaveCategoryAsc();
    List<ErmLeavePolicy> findAllByEnabledTrueOrderByLeaveCategoryAsc();
}
