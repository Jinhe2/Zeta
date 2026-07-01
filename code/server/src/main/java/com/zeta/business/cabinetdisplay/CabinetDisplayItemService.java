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

    public CabinetDisplayItemService(
            ScreenCabinetLookupService screenCabinetLookupService,
            CabinetDisplayItemRepository displayItemRepository,
            CabinetDisplayImageStorage imageStorage) {
        this.screenCabinetLookupService = screenCabinetLookupService;
        this.displayItemRepository = displayItemRepository;
        this.imageStorage = imageStorage;
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
        item.setImageUrl(normalizeImageUrl(request.getImageUrl()));
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
        String nextImageUrl = normalizeImageUrl(request.getImageUrl());

        item.setTitle(request.getTitle().trim());
        item.setImageUrl(nextImageUrl);
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled());

        CabinetDisplayItem saved = displayItemRepository.save(item);
        if (!nextImageUrl.equals(previousImageUrl)) {
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
