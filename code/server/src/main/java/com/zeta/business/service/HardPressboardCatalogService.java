package com.zeta.business.service;

import com.zeta.screen.hardpressboard.HardPressboard;
import com.zeta.screen.hardpressboard.HardPressboardRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HardPressboardCatalogService {
  private final HardPressboardRepository repository;

  public HardPressboardCatalogService(HardPressboardRepository repository) {
    this.repository = repository;
  }

  @Transactional(value = "screenTransactionManager", readOnly = true)
  public List<HardPressboard> list(Long cabinetId) {
    return repository.findByCabinetIdOrderByIdAsc(cabinetId).stream()
        .filter(pb -> pb.getPressboardType() == HardPressboard.PressboardType.FUNCTION
            || pb.getPressboardType() == HardPressboard.PressboardType.EXPORT)
        .collect(Collectors.toList());
  }

  @Transactional(value = "screenTransactionManager", readOnly = true)
  public Map<String, HardPressboard> mapById(Long cabinetId) {
    Map<String, HardPressboard> result = new LinkedHashMap<>();
    for (HardPressboard item : list(cabinetId)) {
      result.put(String.valueOf(item.getId()), item);
    }
    return result;
  }
}
