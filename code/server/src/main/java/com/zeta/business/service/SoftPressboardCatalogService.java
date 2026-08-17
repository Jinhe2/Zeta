package com.zeta.business.service;

import com.zeta.screen.softpressboard.IedSoftPressboardItem;
import com.zeta.screen.softpressboard.IedSoftPressboardItemRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoftPressboardCatalogService {
  private final IedSoftPressboardItemRepository repository;

  public SoftPressboardCatalogService(IedSoftPressboardItemRepository repository) {
    this.repository = repository;
  }

  @Transactional(value = "screenTransactionManager", readOnly = true)
  public List<IedSoftPressboardItem> list(Long iedDeviceId) {
    return repository.findByIedDeviceIdOrderByIdAsc(iedDeviceId);
  }

  @Transactional(value = "screenTransactionManager", readOnly = true)
  public Map<String, IedSoftPressboardItem> mapByReference(Long iedDeviceId) {
    Map<String, IedSoftPressboardItem> result = new LinkedHashMap<>();
    for (IedSoftPressboardItem item : list(iedDeviceId)) {
      result.put(item.getPressboardRef(), item);
    }
    return result;
  }
}
