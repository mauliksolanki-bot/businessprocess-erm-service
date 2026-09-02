package com.org.erm.repository;

import com.org.erm.model.ErmProjectChangeRequestComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmProjectChangeRequestCommentRepository extends JpaRepository<ErmProjectChangeRequestComment, Long> {

    List<ErmProjectChangeRequestComment> findAllByProjectChangeRequestIdOrderByActionAtAscIdAsc(Long projectChangeRequestId);
}
