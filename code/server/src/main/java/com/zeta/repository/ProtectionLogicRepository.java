package com.zeta.repository;

import com.zeta.domain.ProtectionLogic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProtectionLogicRepository extends JpaRepository<ProtectionLogic, Long> {

    List<ProtectionLogic> findByEnabledTrueOrderByIdAsc();

    Optional<ProtectionLogic> findByCode(String code);
}
