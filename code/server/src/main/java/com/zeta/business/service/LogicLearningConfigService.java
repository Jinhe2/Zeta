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
import java.util.*;
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

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public Map<Long, Integer> getWholeExperimentSequences(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        return configRepository.findByLogicDiagramIdIn(ids).stream().collect(Collectors.toMap(
                LogicLearningConfig::getLogicDiagramId,
                config -> config.getWholeExperimentSequence() == null ? 1 : config.getWholeExperimentSequence()));
    }

    @Transactional("businessTransactionManager")
    public List<UpdateLogicLearningConfigsRequest.Item> updateConfigs(
            Long deviceId, List<UpdateLogicLearningConfigsRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请提交需要保存的逻辑配置");
        }
        List<Long> ids = protectionLogicRepository.findByDeviceIdOrderByIdAsc(deviceId).stream()
                .map(ProtectionLogic::getId).collect(Collectors.toList());
        Map<Long, LogicLearningConfig> configs = configRepository.findByLogicDiagramIdIn(ids).stream()
                .collect(Collectors.toMap(LogicLearningConfig::getLogicDiagramId, config -> config));
        Map<Long, Integer> finalOrders = new HashMap<>();
        ids.forEach(id -> finalOrders.put(id, configs.containsKey(id) ? configs.get(id).getSortOrder() : 0));
        Set<Long> submitted = new HashSet<>();
        for (UpdateLogicLearningConfigsRequest.Item item : items) {
            if (item == null || !finalOrders.containsKey(item.getLogicDiagramId()) || !submitted.add(item.getLogicDiagramId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "逻辑编号重复或不属于当前装置，请刷新后重试");
            }
            if (item.getSortOrder() == null || item.getWholeExperimentSequence() == null
                    || item.getWholeExperimentSequence() < 1 || item.getWholeExperimentSequence() > 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写整数排序序号，并选择序列 1、2、3");
            }
            finalOrders.put(item.getLogicDiagramId(), item.getSortOrder());
        }
        for (UpdateLogicLearningConfigsRequest.Item item : items) {
            LogicLearningConfig current = configs.get(item.getLogicDiagramId());
            int previous = current == null ? 0 : current.getSortOrder();
            if (previous != item.getSortOrder() && finalOrders.entrySet().stream().anyMatch(entry ->
                    !entry.getKey().equals(item.getLogicDiagramId()) && entry.getValue().equals(item.getSortOrder()))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "排序编号已存在，请更换编号");
            }
        }
        List<LogicLearningConfig> updates = new ArrayList<>();
        for (UpdateLogicLearningConfigsRequest.Item item : items) {
            LogicLearningConfig config = configs.getOrDefault(item.getLogicDiagramId(), new LogicLearningConfig());
            config.setLogicDiagramId(item.getLogicDiagramId());
            config.setSortOrder(item.getSortOrder());
            config.setWholeExperimentSequence(item.getWholeExperimentSequence());
            updates.add(config);
        }
        configRepository.saveAll(updates);
        configRepository.flush();
        return items;
    }

    @Transactional("businessTransactionManager")
    public int updateWholeExperimentSequence(Long logicDiagramId, int sequence) {
        if (sequence < 1 || sequence > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "整组试验序列只能为 1、2、3");
        }
        if (!protectionLogicRepository.existsById(logicDiagramId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "逻辑框图不存在");
        }
        LogicLearningConfig config = configRepository.findByLogicDiagramId(logicDiagramId)
                .orElseGet(LogicLearningConfig::new);
        config.setLogicDiagramId(logicDiagramId);
        config.setWholeExperimentSequence(sequence);
        return configRepository.save(config).getWholeExperimentSequence();
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
