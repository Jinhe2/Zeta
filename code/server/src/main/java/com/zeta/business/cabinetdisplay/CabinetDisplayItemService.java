package com.zeta.business.cabinetdisplay;

import com.zeta.screen.cabinet.ScreenCabinetLookupService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CabinetDisplayItemService {

    private final ScreenCabinetLookupService screenCabinetLookupService;
    private final CabinetDisplayItemRepository displayItemRepository;
    private final CabinetDisplayImageStorage imageStorage;
    private final TemporaryImageRepository temporaryImageRepository;

    public CabinetDisplayItemService(
            ScreenCabinetLookupService screenCabinetLookupService,
            CabinetDisplayItemRepository displayItemRepository,
            CabinetDisplayImageStorage imageStorage,
            TemporaryImageRepository temporaryImageRepository) {
        this.screenCabinetLookupService = screenCabinetLookupService;
        this.displayItemRepository = displayItemRepository;
        this.imageStorage = imageStorage;
        this.temporaryImageRepository = temporaryImageRepository;
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<CabinetDisplayItemAdminResponse> listByScreenCabinet(Long screenCabinetId) {
        screenCabinetLookupService.requireCabinet(screenCabinetId);
        return displayItemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(screenCabinetId).stream()
                .map(this::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<CabinetDisplayItemResponse> listEnabledByScreenCabinet(Long screenCabinetId) {
        screenCabinetLookupService.requireCabinet(screenCabinetId);
        return displayItemRepository.findByScreenCabinetIdOrderBySortOrderAscIdAsc(screenCabinetId).stream()
                .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
                .map(this::toLearnerResponse)
                .collect(Collectors.toList());
    }

    @Transactional("businessTransactionManager")
    public CabinetDisplayItemAdminResponse create(Long screenCabinetId, CreateCabinetDisplayItemRequest request) {
        screenCabinetLookupService.requireCabinet(screenCabinetId);
        CabinetDisplayItem item = new CabinetDisplayItem();
        item.setScreenCabinetId(screenCabinetId);
        item.setTitle(request.getTitle().trim());
        applyImageData(item, request.getImageId(), request.getImageUrl());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled() == null || request.getEnabled());
        item.setCreatedAt(Instant.now());
        return toAdminResponse(displayItemRepository.save(item));
    }

    @Transactional("businessTransactionManager")
    public CabinetDisplayItemAdminResponse update(Long id, UpdateCabinetDisplayItemRequest request) {
        CabinetDisplayItem item = requireItem(id);
        String previousImageUrl = item.getImageUrl();

        item.setTitle(request.getTitle().trim());
        applyImageData(item, request.getImageId(), request.getImageUrl());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled());

        CabinetDisplayItem saved = displayItemRepository.save(item);
        String nextImageUrl = item.getImageUrl();
        if (nextImageUrl != null && !nextImageUrl.equals(previousImageUrl)) {
            imageStorage.deleteIfManaged(previousImageUrl);
        }
        return toAdminResponse(saved);
    }

    @Transactional("businessTransactionManager")
    public void delete(Long id) {
        CabinetDisplayItem item = requireItem(id);
        imageStorage.deleteIfManaged(item.getImageUrl());
        displayItemRepository.delete(item);
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public CabinetDisplayItemAdminResponse getAdmin(Long id) {
        return toAdminResponse(requireItem(id));
    }

    private CabinetDisplayItem requireItem(Long id) {
        return displayItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜学习条目不存在"));
    }

    /**
     * 应用图片数据到新方式（从临时表）或旧方式（从 URL）。
     * 优先使用 imageId（新方式），如果为空则使用 imageUrl（旧方式）。
     */
    private void applyImageData(CabinetDisplayItem item, Long imageId, String imageUrl) {
        if (imageId != null) {
            // 新方式：从临时表获取图片字节
            TemporaryImage tempImage = temporaryImageRepository.findById(imageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "临时图片不存在或已过期"));
            item.setImageData(tempImage.getImageData());
            item.setImageContentType(tempImage.getContentType());
            item.setImageUrl(null);  // 清空旧 URL
            // 删除临时记录
            temporaryImageRepository.deleteById(imageId);
        } else if (StringUtils.hasText(imageUrl)) {
            // 旧方式：兼容文件系统存储
            item.setImageUrl(normalizeImageUrl(imageUrl));
            item.setImageData(null);
            item.setImageContentType(null);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传认知图片");
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

    private CabinetDisplayItemAdminResponse toAdminResponse(CabinetDisplayItem item) {
        String cabinetName = screenCabinetLookupService.getCabinetName(item.getScreenCabinetId());
        return new CabinetDisplayItemAdminResponse(
                item.getId(),
                item.getScreenCabinetId(),
                cabinetName,
                item.getTitle(),
                item.getImageUrl(),
                item.getContent(),
                item.getSortOrder(),
                item.getEnabled(),
                item.getCreatedAt());
    }

    private CabinetDisplayItemResponse toLearnerResponse(CabinetDisplayItem item) {
        return new CabinetDisplayItemResponse(
                item.getId(),
                item.getTitle(),
                item.getImageUrl(),
                item.getContent(),
                item.getSortOrder());
    }
}
