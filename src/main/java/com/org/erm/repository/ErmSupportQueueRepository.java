package com.org.erm.repository;

import com.org.erm.model.ErmSupportQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErmSupportQueueRepository extends JpaRepository<ErmSupportQueue, Long> {

    Optional<ErmSupportQueue> findByQueueCodeIgnoreCaseAndActiveTrue(String queueCode);

    List<ErmSupportQueue> findAllByActiveTrueOrderByQueueTitleAsc();
}
