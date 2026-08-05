package com.zeta.business.entities.binding;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CabinetBindingRepository extends JpaRepository<CabinetBinding, Long> {

    Optional<CabinetBinding> findByBindId(String bindId);

    Optional<CabinetBinding> findByScreenCabinetId(Long screenCabinetId);

    List<CabinetBinding> findAllByOrderByCreatedAtAsc();

    boolean existsByBindId(String bindId);

    boolean existsByBindLabel(String bindLabel);

    boolean existsByScreenCabinetId(Long screenCabinetId);
}
