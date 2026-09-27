package com.org.erm.repository;

import com.org.erm.model.ErmEmployeeIdSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ErmEmployeeIdSequenceRepository extends JpaRepository<ErmEmployeeIdSequence, String> {

    // Use MySQL's native locking syntax. Hibernate's generated "FOR UPDATE OF alias"
    // is interpreted by MySQL as a table reference and fails during onboarding approval.
    @Query(value = "select * from ERM_EMPLOYEE_ID_SEQUENCES where DESIGNATION_CODE = :designationCode for update",
            nativeQuery = true)
    Optional<ErmEmployeeIdSequence> findByIdForUpdate(String designationCode);
}
