package com.zeta.repository;

import com.zeta.domain.Cabinet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CabinetRepository extends JpaRepository<Cabinet, Long> {

    Optional<Cabinet> findByCode(String code);

    List<Cabinet> findByEnabledTrueOrderBySortOrderAscIdAsc();

    List<Cabinet> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByCode(String code);
}
