package com.org.erm.repository;

import com.org.erm.model.ErmEmployeeIdRoleFormat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ErmEmployeeIdRoleFormatRepository extends JpaRepository<ErmEmployeeIdRoleFormat, String> {

    Optional<ErmEmployeeIdRoleFormat> findByRoleNameIgnoreCase(String roleName);

    List<ErmEmployeeIdRoleFormat> findAllByOrderByPriorityOrderAsc();
}
