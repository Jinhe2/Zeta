package com.zeta.business.logicnodecognition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.business.cabinetdisplay.TemporaryImage;
import com.zeta.business.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.devicedisplay.DeviceDisplayImageStorage;
import com.zeta.business.media.CognitionMediaType;
import com.zeta.business.media.CognitionVideoStorage;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import com.zeta.screen.logicdiagram.dto.ConfigDto;
import com.zeta.screen.logicdiagram.dto.InputDto;
import com.zeta.screen.logicdiagram.dto.OutputDto;
import com.zeta.screen.logicdiagram.dto.TimerDto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LogicNodeCognitionService {

    private final ProtectionLogicRepository protectionLogicRepository;
    private final LogicNodeCognitionItemRepository itemRepository;
    private final TemporaryImageRepository temporaryImageRepository;
    private final DeviceDisplayImageStorage imageStorage;
    private final ObjectMapper objectMapper;
    private final CognitionVideoStorage videoStorage;

    public LogicNodeCognitionService(
            ProtectionLogicRepository protectionLogicRepository,
            LogicNodeCognitionItemRepository itemRepository,
            TemporaryImageRepository temporaryImageRepository,
            DeviceDisplayImageStorage imageStorage,
            ObjectMapper objectMapper,
            CognitionVideoStorage videoStorage) {
        this.protectionLogicRepository = protectionLogicRepository;
        this.itemRepository = itemRepository;
        this.temporaryImageRepository = temporaryImageRepository;
        this.imageStorage = imageStorage;
        this.objectMapper = objectMapper;
        this.videoStorage = videoStorage;
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<LogicNodeSummaryResponse> listConfigurableNodes(Long logicDiagramId) {
        ProtectionLogic logic = requireLogic(logicDiagramId);
        List<LogicNodeInfo> nodes = parseConfigurableNodes(logic);
        return nodes.stream()
                .map(node -> new LogicNodeSummaryResponse(
                        node.getNodeId(),
                        node.getNodeName(),
                        node.getNodeType().name(),
                        itemRepository.countByLogicDiagramIdAndNodeId(logicDiagramId, node.getNodeId())))
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<LogicNodeCognitionItemAdminResponse> listAdminItems(Long logicDiagramId, String nodeId) {
        requireNode(logicDiagramId, nodeId);
        return itemRepository.findByLogicDiagramIdAndNodeIdOrderBySortOrderAscIdAsc(logicDiagramId, nodeId).stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<LogicNodeCognitionItemResponse> listEnabledItems(Long logicDiagramId, String nodeId) {
        requireNode(logicDiagramId, nodeId);
        return itemRepository.findByLogicDiagramIdAndNodeIdOrderBySortOrderAscIdAsc(logicDiagramId, nodeId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(this::toLearnerResponse)
                .collect(Collectors.toList());
    }

    @Transactional("businessTransactionManager")
    public LogicNodeCognitionItemAdminResponse create(
            Long logicDiagramId,
            String nodeId,
            CreateLogicNodeCognitionItemRequest request) {
        LogicNodeInfo node = requireNode(logicDiagramId, nodeId);
        LogicNodeCognitionItem item = new LogicNodeCognitionItem();
        item.setLogicDiagramId(logicDiagramId);
        item.setNodeId(node.getNodeId());
        item.setNodeType(node.getNodeType().name());
        item.setNodeName(node.getNodeName());
        item.setTitle(request.getTitle().trim());
        applyMedia(item, request.getMediaType(), request.getImageId(), request.getImageUrl(),
                request.getVideoPath(), false);
        item.setContent(normalizeContent(request.getContent()));
        validateCognitionContent(item);
        applyHighlightRegion(item, request.getLeftPercent(), request.getTopPercent(),
                request.getWidthPercent(), request.getHeightPercent());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled() == null || request.getEnabled());
        item.setCreatedAt(Instant.now());
        return toAdminResponse(itemRepository.save(item));
    }

    @Transactional("businessTransactionManager")
    public LogicNodeCognitionItemAdminResponse update(Long id, UpdateLogicNodeCognitionItemRequest request) {
        LogicNodeCognitionItem item = requireItem(id);
        LogicNodeInfo node = requireNode(item.getLogicDiagramId(), item.getNodeId());
        String previousImageUrl = item.getImageUrl();
        String previousVideoPath = item.getVideoPath();

        item.setNodeType(node.getNodeType().name());
        item.setNodeName(node.getNodeName());
        item.setTitle(request.getTitle().trim());
        applyMedia(item, request.getMediaType(), request.getImageId(), request.getImageUrl(),
                request.getVideoPath(), Boolean.TRUE.equals(request.getRemoveImage()));
        item.setContent(normalizeContent(request.getContent()));
        validateCognitionContent(item);
        applyHighlightRegion(item, request.getLeftPercent(), request.getTopPercent(),
                request.getWidthPercent(), request.getHeightPercent());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled());

        LogicNodeCognitionItem saved = itemRepository.save(item);
        String nextImageUrl = item.getImageUrl();
        if (!Objects.equals(nextImageUrl, previousImageUrl)) {
            imageStorage.deleteIfManaged(previousImageUrl);
        }
        if (!Objects.equals(item.getVideoPath(), previousVideoPath)) {
            videoStorage.deleteAfterCommit(previousVideoPath);
        }
        return toAdminResponse(saved);
    }

    @Transactional("businessTransactionManager")
    public void delete(Long id) {
        LogicNodeCognitionItem item = requireItem(id);
        imageStorage.deleteIfManaged(item.getImageUrl());
        videoStorage.deleteAfterCommit(item.getVideoPath());
        itemRepository.delete(item);
    }

    private LogicNodeCognitionItem requireItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "逻辑节点认知条目不存在"));
    }

    private LogicNodeInfo requireNode(Long logicDiagramId, String nodeId) {
        if (!StringUtils.hasText(nodeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少节点 ID");
        }
        ProtectionLogic logic = requireLogic(logicDiagramId);
        for (LogicNodeInfo node : parseConfigurableNodes(logic)) {
            if (node.getNodeId().equals(nodeId)) {
                return node;
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "逻辑节点不存在或不可配置");
    }

    private ProtectionLogic requireLogic(Long logicDiagramId) {
        return protectionLogicRepository.findById(logicDiagramId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "逻辑框图不存在"));
    }

    private List<LogicNodeInfo> parseConfigurableNodes(ProtectionLogic logic) {
        ConfigDto config;
        try {
            config = objectMapper.readValue(logic.getConfigJson(), ConfigDto.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "逻辑框图配置解析失败");
        }

        List<LogicNodeInfo> nodes = new ArrayList<>();
        if (config.getInputs() != null) {
            for (InputDto input : config.getInputs()) {
                addNode(nodes, input.getId(), input.getName(), LogicNodeType.INPUT);
            }
        }
        if (config.getTimers() != null) {
            for (TimerDto timer : config.getTimers()) {
                addNode(nodes, timer.getId(), timer.getName(), LogicNodeType.TIMER);
            }
        }
        if (config.getOutputs() != null) {
            for (OutputDto output : config.getOutputs()) {
                addNode(nodes, output.getId(), output.getName(), LogicNodeType.OUTPUT);
            }
        }
        return nodes;
    }

    private void addNode(List<LogicNodeInfo> nodes, String nodeId, String nodeName, LogicNodeType nodeType) {
        if (!StringUtils.hasText(nodeId)) {
            return;
        }
        String normalizedId = nodeId.trim();
        boolean exists = nodes.stream().anyMatch(node -> node.getNodeId().equals(normalizedId));
        if (exists) {
            return;
        }
        String resolvedName = StringUtils.hasText(nodeName) ? nodeName.trim() : normalizedId;
        nodes.add(new LogicNodeInfo(normalizedId, resolvedName, nodeType));
    }

    private void applyMedia(LogicNodeCognitionItem item, CognitionMediaType mediaType, Long imageId,
                            String imageUrl, String videoPath, boolean removeImage) {
        item.setMediaType(mediaType);
        if (mediaType == CognitionMediaType.VIDEO) {
            String normalized = videoStorage.normalizeManagedPath(videoPath);
            if (!videoStorage.exists(normalized)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "认知视频不存在，请重新上传");
            }
            item.setVideoPath(normalized);
            clearImage(item);
            return;
        }
        item.setVideoPath(null);
        if (mediaType == CognitionMediaType.TEXT) {
            clearImage(item);
            return;
        }
        applyImageData(item, imageId, imageUrl, removeImage);
    }

    private void applyImageData(LogicNodeCognitionItem item, Long imageId, String imageUrl, boolean removeImage) {
        if (removeImage) {
            item.setImageUrl(null);
            item.setImageData(null);
            item.setImageContentType(null);
        } else if (imageId != null) {
            TemporaryImage tempImage = temporaryImageRepository.findById(imageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "临时图片不存在或已过期"));
            item.setImageData(tempImage.getImageData());
            item.setImageContentType(tempImage.getContentType());
            item.setImageUrl(null);
            temporaryImageRepository.deleteById(imageId);
        } else if (StringUtils.hasText(imageUrl)) {
            item.setImageUrl(normalizeImageUrl(imageUrl));
            item.setImageData(null);
            item.setImageContentType(null);
        } else if (hasExistingImage(item)) {
            return;
        }
    }

    private void clearImage(LogicNodeCognitionItem item) {
        item.setImageUrl(null);
        item.setImageData(null);
        item.setImageContentType(null);
        item.setLeftPercent(null);
        item.setTopPercent(null);
        item.setWidthPercent(null);
        item.setHeightPercent(null);
    }

    private boolean hasExistingImage(LogicNodeCognitionItem item) {
        return StringUtils.hasText(item.getImageUrl())
                || (item.getImageData() != null && item.getImageData().length > 0);
    }

    private String normalizeContent(String content) {
        return StringUtils.hasText(content) ? content.trim() : "";
    }

    private void validateCognitionContent(LogicNodeCognitionItem item) {
        if (item.getMediaType() == CognitionMediaType.VIDEO && !StringUtils.hasText(item.getVideoPath())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传认知视频");
        }
        if (item.getMediaType() == CognitionMediaType.IMAGE && !hasExistingImage(item)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传认知图片");
        }
        if (item.getMediaType() == CognitionMediaType.TEXT && !StringUtils.hasText(item.getContent())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写文字描述");
        }
    }

    private void applyHighlightRegion(
            LogicNodeCognitionItem item,
            Double left,
            Double top,
            Double width,
            Double height) {
        if (item.getMediaType() != CognitionMediaType.IMAGE) {
            item.setLeftPercent(null);
            item.setTopPercent(null);
            item.setWidthPercent(null);
            item.setHeightPercent(null);
            return;
        }
        if (left == null && top == null && width == null && height == null) {
            item.setLeftPercent(null);
            item.setTopPercent(null);
            item.setWidthPercent(null);
            item.setHeightPercent(null);
            return;
        }
        if (left == null || top == null || width == null || height == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "高亮区域坐标不完整");
        }
        if (!hasExistingImage(item)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请先上传认知图片后再设置高亮区域");
        }
        validateRegion(left, top, width, height);
        item.setLeftPercent(left);
        item.setTopPercent(top);
        item.setWidthPercent(width);
        item.setHeightPercent(height);
    }

    private void validateRegion(Double left, Double top, Double width, Double height) {
        if (left < 0 || top < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "高亮区域坐标不能小于 0");
        }
        if (width <= 0 || height <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "高亮区域宽高必须大于 0");
        }
        if (left + width > 100.01 || top + height > 100.01) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "高亮区域超出图片范围");
        }
    }

    private String normalizeImageUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传认知图片");
        }
        String trimmed = imageUrl.trim();
        if (!trimmed.startsWith("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片地址格式不正确");
        }
        return trimmed;
    }

    private LogicNodeCognitionItemAdminResponse toAdminResponse(LogicNodeCognitionItem item) {
        return new LogicNodeCognitionItemAdminResponse(
                item.getId(),
                item.getLogicDiagramId(),
                item.getNodeId(),
                item.getNodeType(),
                item.getNodeName(),
                item.getTitle(),
                item.getImageUrl(),
                hasExistingImage(item),
                item.getMediaType(),
                item.getVideoPath(),
                item.getLeftPercent(),
                item.getTopPercent(),
                item.getWidthPercent(),
                item.getHeightPercent(),
                item.getContent(),
                item.getSortOrder(),
                item.getEnabled(),
                item.getCreatedAt());
    }

    private LogicNodeCognitionItemResponse toLearnerResponse(LogicNodeCognitionItem item) {
        return new LogicNodeCognitionItemResponse(
                item.getId(),
                item.getTitle(),
                item.getImageUrl(),
                hasExistingImage(item),
                item.getMediaType(),
                item.getLeftPercent(),
                item.getTopPercent(),
                item.getWidthPercent(),
                item.getHeightPercent(),
                item.getContent(),
                item.getSortOrder());
    }

    private static class LogicNodeInfo {
        private final String nodeId;
        private final String nodeName;
        private final LogicNodeType nodeType;

        LogicNodeInfo(String nodeId, String nodeName, LogicNodeType nodeType) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.nodeType = nodeType;
        }

        String getNodeId() {
            return nodeId;
        }

        String getNodeName() {
            return nodeName;
        }

        LogicNodeType getNodeType() {
            return nodeType;
        }
    }
}
