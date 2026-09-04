package com.org.erm.repository;

import com.org.erm.model.ErmSupportCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErmSupportCategoryRepository extends JpaRepository<ErmSupportCategory, Long> {

    List<ErmSupportCategory> findAllByActiveTrueOrderBySortOrderAscCategoryTitleAsc();

    Optional<ErmSupportCategory> findByCategoryCodeIgnoreCaseAndActiveTrue(String categoryCode);
}
