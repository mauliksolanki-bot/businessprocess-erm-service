package com.org.erm.repository;

import com.org.erm.model.ErmEmployeeProfileUpdateRequestComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmEmployeeProfileUpdateRequestCommentRepository extends JpaRepository<ErmEmployeeProfileUpdateRequestComment, Long> {
    List<ErmEmployeeProfileUpdateRequestComment> findAllByEmployeeProfileUpdateRequestIdOrderByActionAtAscIdAsc(Long employeeProfileUpdateRequestId);
}
