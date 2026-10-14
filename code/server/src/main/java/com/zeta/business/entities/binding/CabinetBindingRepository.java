package com.zeta.business.entities.binding;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;

public interface CabinetBindingRepository extends JpaRepository<CabinetBinding, Long> {

    Optional<CabinetBinding> findByBindId(String bindId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CabinetBinding b where b.bindId = :bindId")
    Optional<CabinetBinding> findByBindIdForActivityUpdate(@Param("bindId") String bindId);

    Optional<CabinetBinding> findByScreenCabinetId(Long screenCabinetId);

    List<CabinetBinding> findAllByOrderByCreatedAtAsc();

    boolean existsByBindId(String bindId);

    boolean existsByBindLabel(String bindLabel);

    boolean existsByScreenCabinetId(Long screenCabinetId);
}
