package com.zeta.business.service;

import com.zeta.business.entities.binding.*;
import com.zeta.business.entities.binding.dto.*;
import com.zeta.business.entities.cabinetdisplay.*;
import com.zeta.business.entities.cabinetdisplay.dto.*;
import com.zeta.business.entities.cognitiondevice.*;
import com.zeta.business.entities.cognitiondevice.dto.*;
import com.zeta.business.entities.devicedisplay.*;
import com.zeta.business.entities.devicedisplay.dto.*;
import com.zeta.business.entities.drawinglearning.*;
import com.zeta.business.entities.drawinglearning.dto.*;
import com.zeta.business.entities.learningresource.*;
import com.zeta.business.entities.learningresource.dto.*;
import com.zeta.business.entities.logiclearning.*;
import com.zeta.business.entities.logiclearning.dto.*;
import com.zeta.business.entities.logicnodecognition.*;
import com.zeta.business.entities.logicnodecognition.dto.*;
import com.zeta.business.entities.monitor.*;
import com.zeta.business.entities.snapshot.*;
import com.zeta.business.entities.snapshot.dto.*;
import com.zeta.business.entities.user.*;
import com.zeta.business.entities.user.dto.*;
import com.zeta.business.media.*;
import com.zeta.business.storage.*;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        ProtectionLogic logic = protectionLogicRepository.findById(logicDiagramId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "逻辑框图不存在"));
        LogicLearningConfig config = configRepository.findByLogicDiagramId(logicDiagramId)
                .orElseGet(LogicLearningConfig::new);
        Integer currentSortOrder = config.getSortOrder();
        List<Long> siblingLogicIds = protectionLogicRepository
                .findByDeviceIdOrderByIdAsc(logic.getDevice().getId())
                .stream()
                .map(ProtectionLogic::getId)
                .collect(Collectors.toList());
        List<LogicLearningConfig> siblingConfigs = configRepository.findByLogicDiagramIdIn(siblingLogicIds);
        config.setLogicDiagramId(logicDiagramId);
        config.setSortOrder(SortOrderHelper.resolveForUpdate(
                sortOrder,
                currentSortOrder,
                siblingConfigs,
                LogicLearningConfig::getSortOrder,
                LogicLearningConfig::getLogicDiagramId,
                logicDiagramId));
        return configRepository.save(config).getSortOrder();
    }
}
