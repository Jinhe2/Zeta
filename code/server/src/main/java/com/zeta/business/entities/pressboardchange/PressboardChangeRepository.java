package com.zeta.business.entities.pressboardchange;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PressboardChangeRepository extends JpaRepository<PressboardChange, Long> {

    List<PressboardChange> findByCabinetIdOrderByChangeTimeMsDescIdDesc(Long cabinetId);
}
