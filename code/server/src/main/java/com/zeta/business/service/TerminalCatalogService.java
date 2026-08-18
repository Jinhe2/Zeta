package com.zeta.business.service;

import com.zeta.screen.terminal.Terminal;
import com.zeta.screen.terminal.TerminalRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 端子目录只读查询（屏柜库），供业务层跨库组装响应与校验。 */
@Service
public class TerminalCatalogService {
  private final TerminalRepository repository;

  public TerminalCatalogService(TerminalRepository repository) {
    this.repository = repository;
  }

  @Transactional(value = "screenTransactionManager", readOnly = true)
  public Map<Long, Terminal> byId(Collection<Long> ids) {
    if (ids == null || ids.isEmpty()) return Collections.emptyMap();
    Map<Long, Terminal> result = new LinkedHashMap<>();
    for (Terminal terminal : repository.findAllWithCabinetAndStripByIdIn(ids)) {
      result.put(terminal.getId(), terminal);
    }
    return result;
  }
}
