package com.zeta.business.service;

import com.zeta.screen.iedsetting.IedSettingItem;
import com.zeta.screen.iedsetting.IedSettingItemRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingCatalogService {
  private final IedSettingItemRepository repository;

  public SettingCatalogService(IedSettingItemRepository repository) {
    this.repository = repository;
  }

  @Transactional(value = "screenTransactionManager", readOnly = true)
  public List<IedSettingItem> list(Long iedDeviceId) {
    return repository.findByIedDeviceIdOrderByIdAsc(iedDeviceId);
  }

  @Transactional(value = "screenTransactionManager", readOnly = true)
  public Map<String, IedSettingItem> mapByReference(Long iedDeviceId) {
    Map<String, IedSettingItem> result = new LinkedHashMap<>();
    for (IedSettingItem item : list(iedDeviceId)) {
      result.put(item.getSettingRef(), item);
    }
    return result;
  }
}
