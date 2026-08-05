package com.zeta.screen.cabinet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CabinetRepository extends JpaRepository<Cabinet, Long> {

    Optional<Cabinet> findByName(String name);

    Optional<Cabinet> findByLocation(String location);

    List<Cabinet> findAllByOrderByIdAsc();

    boolean existsByName(String name);
}
