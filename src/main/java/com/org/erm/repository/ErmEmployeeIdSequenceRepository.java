package com.org.erm.repository;

import com.org.erm.model.ErmEmployeeIdSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ErmEmployeeIdSequenceRepository extends JpaRepository<ErmEmployeeIdSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ErmEmployeeIdSequence s where s.designationCode = :designationCode")
    Optional<ErmEmployeeIdSequence> findByIdForUpdate(String designationCode);
}
