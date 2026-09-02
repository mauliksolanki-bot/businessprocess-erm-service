package com.org.erm.repository;

import com.org.erm.model.ErmProjectRequestComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErmProjectRequestCommentRepository extends JpaRepository<ErmProjectRequestComment, Long> {

    List<ErmProjectRequestComment> findAllByProjectRequestIdOrderByActionAtAscIdAsc(Long projectRequestId);
}
