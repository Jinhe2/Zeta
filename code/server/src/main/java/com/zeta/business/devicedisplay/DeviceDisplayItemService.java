package com.zeta.business.devicedisplay;

import com.zeta.business.cabinetdisplay.TemporaryImage;
import com.zeta.business.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.cognitiondevice.CognitionDevice;
import com.zeta.business.cognitiondevice.CognitionDeviceType;
import com.zeta.business.cognitiondevice.CognitionDeviceService;
import com.zeta.business.cabinetdisplay.CabinetDisplayItem;
import com.zeta.business.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.business.media.CognitionMediaType;
import com.zeta.business.media.CognitionVideoStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import com.zeta.screen.terminal.Terminal;
import com.zeta.screen.terminal.TerminalRepository;
import com.zeta.screen.terminal.TerminalStrip;
import com.zeta.screen.terminal.TerminalStripRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DeviceDisplayItemService {

    private final CognitionDeviceService cognitionDeviceService;
    private final DeviceDisplayItemRepository displayItemRepository;
    private final DeviceDisplayImageStorage imageStorage;
    private final TemporaryImageRepository temporaryImageRepository;
    private final CognitionVideoStorage videoStorage;
    private final TerminalOperationRepository terminalOperationRepository;
    private final TerminalOperationTerminalRepository terminalOperationTerminalRepository;
    private final TerminalStripRepository terminalStripRepository;
    private final TerminalRepository terminalRepository;
    private final CabinetDisplayItemRepository cabinetDisplayItemRepository;

    public DeviceDisplayItemService(
            CognitionDeviceService cognitionDeviceService,
            DeviceDisplayItemRepository displayItemRepository,
            DeviceDisplayImageStorage imageStorage,
            TemporaryImageRepository temporaryImageRepository,
            CognitionVideoStorage videoStorage,
            TerminalOperationRepository terminalOperationRepository,
            TerminalOperationTerminalRepository terminalOperationTerminalRepository,
            TerminalStripRepository terminalStripRepository,
            TerminalRepository terminalRepository,
            CabinetDisplayItemRepository cabinetDisplayItemRepository) {
        this.cognitionDeviceService = cognitionDeviceService;
        this.displayItemRepository = displayItemRepository;
        this.imageStorage = imageStorage;
        this.temporaryImageRepository = temporaryImageRepository;
        this.videoStorage = videoStorage;
        this.terminalOperationRepository = terminalOperationRepository;
        this.terminalOperationTerminalRepository = terminalOperationTerminalRepository;
        this.terminalStripRepository = terminalStripRepository;
        this.terminalRepository = terminalRepository;
        this.cabinetDisplayItemRepository = cabinetDisplayItemRepository;
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<DeviceDisplayItemAdminResponse> listByCognitionDevice(Long cognitionDeviceId) {
        cognitionDeviceService.requireDevice(cognitionDeviceId);
        return displayItemRepository.findByCognitionDeviceIdOrderBySortOrderAscIdAsc(cognitionDeviceId).stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<DeviceDisplayItemResponse> listEnabledByCognitionDevice(Long cognitionDeviceId) {
        cognitionDeviceService.requireDevice(cognitionDeviceId);
        return displayItemRepository.findByCognitionDeviceIdOrderBySortOrderAscIdAsc(cognitionDeviceId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(this::toLearnerResponse)
                .collect(Collectors.toList());
    }

    @Transactional("businessTransactionManager")
    public DeviceDisplayItemAdminResponse create(Long cognitionDeviceId, CreateDeviceDisplayItemRequest request) {
        cognitionDeviceService.requireDevice(cognitionDeviceId);
        DeviceDisplayItem item = new DeviceDisplayItem();
        item.setCognitionDeviceId(cognitionDeviceId);
        item.setTitle(request.getTitle().trim());
        validateTerminalOperationType(cognitionDeviceId, request.getMediaType());
        applyMedia(item, request.getMediaType(), request.getImageId(), request.getImageUrl(), request.getVideoPath());
        applyHighlightRegion(item, request.getLeftPercent(), request.getTopPercent(),
                request.getWidthPercent(), request.getHeightPercent());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled() == null || request.getEnabled());
        item.setCreatedAt(Instant.now());
        DeviceDisplayItem saved = displayItemRepository.save(item);
        replaceTerminalOperation(saved, request.getTerminalOperation());
        return toAdminResponse(saved);
    }

    @Transactional("businessTransactionManager")
    public DeviceDisplayItemAdminResponse update(Long id, UpdateDeviceDisplayItemRequest request) {
        DeviceDisplayItem item = requireItem(id);
        String previousImageUrl = item.getImageUrl();
        String previousVideoPath = item.getVideoPath();

        validateTerminalOperationType(item.getCognitionDeviceId(), request.getMediaType());
        item.setTitle(request.getTitle().trim());
        applyMedia(item, request.getMediaType(), request.getImageId(), request.getImageUrl(), request.getVideoPath());
        applyHighlightRegion(item, request.getLeftPercent(), request.getTopPercent(),
                request.getWidthPercent(), request.getHeightPercent());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled());

        DeviceDisplayItem saved = displayItemRepository.save(item);
        replaceTerminalOperation(saved, request.getTerminalOperation());
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
        DeviceDisplayItem item = requireItem(id);
        deleteTerminalOperation(item.getId());
        imageStorage.deleteIfManaged(item.getImageUrl());
        videoStorage.deleteAfterCommit(item.getVideoPath());
        displayItemRepository.delete(item);
    }

    private DeviceDisplayItem requireItem(Long id) {
        return displayItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备展示条目不存在"));
    }

    private void applyMedia(DeviceDisplayItem item, CognitionMediaType mediaType, Long imageId,
                            String imageUrl, String videoPath) {
        if (mediaType == CognitionMediaType.TEXT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "设备认知条目不支持纯文字类型");
        }
        if (mediaType == CognitionMediaType.TERMINAL_OPERATION) {
            item.setMediaType(mediaType);
            item.setVideoPath(null);
            clearImage(item);
            return;
        }
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
        applyImageData(item, imageId, imageUrl);
    }

    private void applyImageData(DeviceDisplayItem item, Long imageId, String imageUrl) {
        if (imageId != null) {
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
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传认知图片");
        }
    }

    private void clearImage(DeviceDisplayItem item) {
        item.setImageUrl(null);
        item.setImageData(null);
        item.setImageContentType(null);
        item.setLeftPercent(null);
        item.setTopPercent(null);
        item.setWidthPercent(null);
        item.setHeightPercent(null);
    }

    private boolean hasExistingImage(DeviceDisplayItem item) {
        return StringUtils.hasText(item.getImageUrl())
                || (item.getImageData() != null && item.getImageData().length > 0);
    }

    private void applyHighlightRegion(
            DeviceDisplayItem item,
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

    private DeviceDisplayItemAdminResponse toAdminResponse(DeviceDisplayItem item) {
        CognitionDevice cognitionDevice = cognitionDeviceService.requireDevice(item.getCognitionDeviceId());
        return new DeviceDisplayItemAdminResponse(
                item.getId(),
                item.getCognitionDeviceId(),
                cognitionDevice.getTitle(),
                item.getTitle(),
                item.getImageUrl(),
                item.getMediaType(),
                item.getVideoPath(),
                item.getLeftPercent(),
                item.getTopPercent(),
                item.getWidthPercent(),
                item.getHeightPercent(),
                item.getContent(),
                item.getSortOrder(),
                item.getEnabled(),
                item.getCreatedAt(),
                terminalOperationResponse(item));
    }

    private DeviceDisplayItemResponse toLearnerResponse(DeviceDisplayItem item) {
        return new DeviceDisplayItemResponse(
                item.getId(),
                item.getTitle(),
                item.getImageUrl(),
                item.getMediaType(),
                item.getLeftPercent(),
                item.getTopPercent(),
                item.getWidthPercent(),
                item.getHeightPercent(),
                item.getContent(),
                item.getSortOrder(),
                terminalOperationResponse(item));
    }

    private void validateTerminalOperationType(Long cognitionDeviceId, CognitionMediaType mediaType) {
        if (mediaType != CognitionMediaType.TERMINAL_OPERATION) return;
        CognitionDevice device = cognitionDeviceService.requireDevice(cognitionDeviceId);
        if (device.getDeviceType() != CognitionDeviceType.TERMINAL_GROUP) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "端子操作条目仅适用于端子组");
        }
    }

    private void replaceTerminalOperation(DeviceDisplayItem item, TerminalOperationRequest request) {
        if (item.getMediaType() != CognitionMediaType.TERMINAL_OPERATION) {
            deleteTerminalOperation(item.getId());
            return;
        }
        if (request == null || request.getTerminalStripId() == null || request.getTerminals() == null || request.getTerminals().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "端子操作须选择端子排和至少一个端子");
        }
        CognitionDevice device = cognitionDeviceService.requireDevice(item.getCognitionDeviceId());
        CabinetDisplayItem cabinetItem = cabinetDisplayItemRepository.findById(device.getCabinetDisplayItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜学习条目不存在"));
        TerminalStrip strip = terminalStripRepository.findById(request.getTerminalStripId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "端子排不存在"));
        if (!Objects.equals(strip.getCabinet().getId(), cabinetItem.getScreenCabinetId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "端子排不属于当前屏柜");
        }
        deleteTerminalOperation(item.getId());
        TerminalOperation operation = new TerminalOperation();
        operation.setDeviceDisplayItemId(item.getId());
        operation.setTerminalStripId(strip.getId());
        operation = terminalOperationRepository.save(operation);
        int sortOrder = 0;
        java.util.HashSet<Long> terminalIds = new java.util.HashSet<>();
        for (TerminalOperationTerminalRequest selected : request.getTerminals()) {
            if (selected == null || selected.getTerminalId() == null || !StringUtils.hasText(selected.getMeaning())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "端子的表示含义不能为空");
            }
            if (!terminalIds.add(selected.getTerminalId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "端子不能重复选择");
            }
            Terminal terminal = terminalRepository.findById(selected.getTerminalId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "端子不存在"));
            if (terminal.getTerminalStrip() == null || !Objects.equals(terminal.getTerminalStrip().getId(), strip.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "所选端子不属于端子排");
            }
            TerminalOperationTerminal operationTerminal = new TerminalOperationTerminal();
            operationTerminal.setTerminalOperationId(operation.getId());
            operationTerminal.setTerminalId(terminal.getId());
            operationTerminal.setTerminalMeaning(selected.getMeaning().trim());
            operationTerminal.setSortOrder(sortOrder++);
            terminalOperationTerminalRepository.save(operationTerminal);
        }
    }

    private void deleteTerminalOperation(Long displayItemId) {
        terminalOperationRepository.findByDeviceDisplayItemId(displayItemId).ifPresent(operation -> {
            terminalOperationTerminalRepository.deleteByTerminalOperationId(operation.getId());
            terminalOperationRepository.delete(operation);
        });
    }

    private TerminalOperationResponse terminalOperationResponse(DeviceDisplayItem item) {
        if (item.getMediaType() != CognitionMediaType.TERMINAL_OPERATION) return null;
        TerminalOperation operation = terminalOperationRepository.findByDeviceDisplayItemId(item.getId()).orElse(null);
        if (operation == null) return null;
        TerminalStrip strip = terminalStripRepository.findById(operation.getTerminalStripId()).orElse(null);
        List<TerminalOperationTerminalResponse> terminals = terminalOperationTerminalRepository
                .findByTerminalOperationIdOrderBySortOrderAscIdAsc(operation.getId()).stream()
                .map(selected -> {
                    Terminal terminal = terminalRepository.findById(selected.getTerminalId()).orElse(null);
                    return terminal == null ? null : new TerminalOperationTerminalResponse(
                            terminal.getId(), terminal.getTerminalLabel(), selected.getTerminalMeaning());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new TerminalOperationResponse(operation.getTerminalStripId(),
                strip == null ? null : strip.getName(), strip == null ? null : strip.getLabelPrefix(), terminals);
    }
}
