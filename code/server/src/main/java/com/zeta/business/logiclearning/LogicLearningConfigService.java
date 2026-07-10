package com.zeta.business.logiclearning;

import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LogicLearningConfigService {

    private final LogicLearningConfigRepository configRepository;
    private final ProtectionLogicRepository protectionLogicRepository;

    public LogicLearningConfigService(
            LogicLearningConfigRepository configRepository,
            ProtectionLogicRepository protectionLogicRepository) {
        this.configRepository = configRepository;
        this.protectionLogicRepository = protectionLogicRepository;
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public Map<Long, Integer> getSortOrders(Collection<Long> logicDiagramIds) {
        if (logicDiagramIds == null || logicDiagramIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return configRepository.findByLogicDiagramIdIn(logicDiagramIds).stream()
                .collect(Collectors.toMap(
                        LogicLearningConfig::getLogicDiagramId,
                        LogicLearningConfig::getSortOrder));
    }

    @Transactional("businessTransactionManager")
    public int updateSortOrder(Long logicDiagramId, int sortOrder) {
        if (!protectionLogicRepository.existsById(logicDiagramId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "逻辑框图不存在");
        }
        LogicLearningConfig config = configRepository.findByLogicDiagramId(logicDiagramId)
                .orElseGet(LogicLearningConfig::new);
        config.setLogicDiagramId(logicDiagramId);
        config.setSortOrder(sortOrder);
        return configRepository.save(config).getSortOrder();
    }
}
