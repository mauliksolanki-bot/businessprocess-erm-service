package com.org.erm.repository;

import com.org.erm.model.ErmRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErmRoleRepository extends JpaRepository<ErmRole, Long> {

    Optional<ErmRole> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<ErmRole> findAllByOrderByNameAsc();
}
