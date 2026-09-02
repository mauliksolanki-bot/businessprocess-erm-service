package com.org.erm.repository;

import com.org.erm.model.ErmProjectAllocationComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmProjectAllocationCommentRepository extends JpaRepository<ErmProjectAllocationComment, Long> {

    List<ErmProjectAllocationComment> findAllByProjectAllocationIdOrderByActionAtAscIdAsc(Long projectAllocationId);
}
