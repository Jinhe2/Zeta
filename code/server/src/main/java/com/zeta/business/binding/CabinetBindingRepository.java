package com.zeta.business.binding;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CabinetBindingRepository extends JpaRepository<CabinetBinding, Long> {

    Optional<CabinetBinding> findByBindId(String bindId);

    Optional<CabinetBinding> findByScreenCabinetId(Long screenCabinetId);

    List<CabinetBinding> findAllByOrderByCreatedAtAsc();

    boolean existsByBindId(String bindId);

    boolean existsByBindLabel(String bindLabel);

    boolean existsByScreenCabinetId(Long screenCabinetId);
}
