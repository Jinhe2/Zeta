package com.zeta.business.cabinetdisplay;

import com.zeta.screen.cabinet.ScreenCabinetLookupService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CabinetDisplayItemService {

    private final ScreenCabinetLookupService screenCabinetLookupService;
    private final CabinetDisplayItemRepository displayItemRepository;

    public CabinetDisplayItemService(
            ScreenCabinetLookupService screenCabinetLookupService,
            CabinetDisplayItemRepository displayItemRepository) {
        this.screenCabinetLookupService = screenCabinetLookupService;
        this.displayItemRepository = displayItemRepository;
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
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled() == null || request.getEnabled());
        item.setCreatedAt(Instant.now());
        return toAdminResponse(displayItemRepository.save(item));
    }

    @Transactional("businessTransactionManager")
    public CabinetDisplayItemAdminResponse update(Long id, UpdateCabinetDisplayItemRequest request) {
        CabinetDisplayItem item = requireItem(id);
        item.setTitle(request.getTitle().trim());
        item.setContent(request.getContent().trim());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled());
        return toAdminResponse(displayItemRepository.save(item));
    }

    @Transactional("businessTransactionManager")
    public void delete(Long id) {
        CabinetDisplayItem item = requireItem(id);
        displayItemRepository.delete(item);
    }

    private CabinetDisplayItem requireItem(Long id) {
        return displayItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜展示条目不存在"));
    }

    private CabinetDisplayItemAdminResponse toAdminResponse(CabinetDisplayItem item) {
        String cabinetName = screenCabinetLookupService.getCabinetName(item.getScreenCabinetId());
        return new CabinetDisplayItemAdminResponse(
                item.getId(),
                item.getScreenCabinetId(),
                cabinetName,
                item.getTitle(),
                item.getContent(),
                item.getSortOrder(),
                item.getEnabled(),
                item.getCreatedAt());
    }

    private CabinetDisplayItemResponse toLearnerResponse(CabinetDisplayItem item) {
        return new CabinetDisplayItemResponse(
                item.getId(),
                item.getTitle(),
                item.getContent(),
                item.getSortOrder());
    }
}
