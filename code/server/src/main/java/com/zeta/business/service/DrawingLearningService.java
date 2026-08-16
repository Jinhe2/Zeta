package com.zeta.business.service;

import com.zeta.business.entities.binding.*;
import com.zeta.business.entities.binding.dto.*;
import com.zeta.business.entities.cabinetdisplay.*;
import com.zeta.business.entities.cabinetdisplay.TemporaryImage;
import com.zeta.business.entities.cabinetdisplay.TemporaryImageRepository;
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
import com.zeta.business.storage.CabinetDisplayImageStorage;
import com.zeta.screen.cabinet.ScreenCabinetLookupService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DrawingLearningService {

    private final ScreenCabinetLookupService screenCabinetLookupService;
    private final DrawingGroupRepository groupRepository;
    private final DrawingPageRepository pageRepository;
    private final DrawingCognitionItemRepository itemRepository;
    private final CabinetDisplayImageStorage imageStorage;
    private final TemporaryImageRepository temporaryImageRepository;
    private final SharedMediaCleanupService mediaCleanupService;

    public DrawingLearningService(
            ScreenCabinetLookupService screenCabinetLookupService,
            DrawingGroupRepository groupRepository,
            DrawingPageRepository pageRepository,
            DrawingCognitionItemRepository itemRepository,
            CabinetDisplayImageStorage imageStorage,
            TemporaryImageRepository temporaryImageRepository,
            SharedMediaCleanupService mediaCleanupService) {
        this.screenCabinetLookupService = screenCabinetLookupService;
        this.groupRepository = groupRepository;
        this.pageRepository = pageRepository;
        this.itemRepository = itemRepository;
        this.imageStorage = imageStorage;
        this.temporaryImageRepository = temporaryImageRepository;
        this.mediaCleanupService = mediaCleanupService;
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<DrawingGroupAdminResponse> listGroups(Long cabinetId) {
        screenCabinetLookupService.requireCabinet(cabinetId);
        return groupRepository.findByScreenCabinetIdOrderByDrawingTypeAscSortOrderAscIdAsc(cabinetId).stream()
                .map(this::toAdminGroupResponse)
                .collect(Collectors.toList());
    }

    @Transactional("businessTransactionManager")
    public DrawingGroupAdminResponse createGroup(Long cabinetId, CreateDrawingGroupRequest request) {
        screenCabinetLookupService.requireCabinet(cabinetId);
        DrawingGroup group = new DrawingGroup();
        group.setScreenCabinetId(cabinetId);
        group.setDrawingType(request.getDrawingType());
        group.setName(request.getName().trim());
        group.setSortOrder(request.getSortOrder());
        group.setEnabled(request.getEnabled() == null || request.getEnabled());
        group.setCreatedAt(Instant.now());
        return toAdminGroupResponse(groupRepository.save(group));
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public DrawingGroupAdminResponse getGroup(Long id) {
        return toAdminGroupResponse(requireGroup(id));
    }

    @Transactional("businessTransactionManager")
    public DrawingGroupAdminResponse updateGroup(Long id, UpdateDrawingGroupRequest request) {
        DrawingGroup group = requireGroup(id);
        group.setDrawingType(request.getDrawingType());
        group.setName(request.getName().trim());
        group.setSortOrder(request.getSortOrder());
        group.setEnabled(request.getEnabled());
        return toAdminGroupResponse(groupRepository.save(group));
    }

    @Transactional("businessTransactionManager")
    public void deleteGroup(Long id) {
        DrawingGroup group = requireGroup(id);
        List<DrawingPage> pages = pageRepository.findByDrawingGroupIdOrderBySortOrderAscIdAsc(id);
        List<Long> pageIds = pages.stream().map(DrawingPage::getId).collect(Collectors.toList());
        if (!pageIds.isEmpty()) {
            itemRepository.deleteByDrawingPageIdIn(pageIds);
        }
        pages.forEach(page -> mediaCleanupService.scheduleCabinetImageDeletion(page.getImageUrl()));
        pageRepository.deleteByDrawingGroupId(id);
        groupRepository.delete(group);
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<DrawingPageAdminResponse> listPages(Long groupId) {
        requireGroup(groupId);
        return pageRepository.findByDrawingGroupIdOrderBySortOrderAscIdAsc(groupId).stream()
                .map(this::toAdminPageResponse)
                .collect(Collectors.toList());
    }

    @Transactional("businessTransactionManager")
    public DrawingPageAdminResponse createPage(Long groupId, CreateDrawingPageRequest request) {
        requireGroup(groupId);
        DrawingPage page = new DrawingPage();
        page.setDrawingGroupId(groupId);
        page.setTitle(request.getTitle().trim());
        applyImageData(page, request.getImageId(), request.getImageUrl());
        page.setSortOrder(request.getSortOrder());
        page.setEnabled(request.getEnabled() == null || request.getEnabled());
        page.setCreatedAt(Instant.now());
        return toAdminPageResponse(pageRepository.save(page));
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public DrawingPageAdminResponse getPage(Long id) {
        return toAdminPageResponse(requirePage(id));
    }

    @Transactional("businessTransactionManager")
    public DrawingPageAdminResponse updatePage(Long id, UpdateDrawingPageRequest request) {
        DrawingPage page = requirePage(id);
        String previousImageUrl = page.getImageUrl();
        page.setTitle(request.getTitle().trim());
        applyImageData(page, request.getImageId(), request.getImageUrl());
        page.setSortOrder(request.getSortOrder());
        page.setEnabled(request.getEnabled());
        DrawingPage saved = pageRepository.save(page);
        if (!Objects.equals(previousImageUrl, page.getImageUrl())) {
            mediaCleanupService.scheduleCabinetImageDeletion(previousImageUrl);
        }
        return toAdminPageResponse(saved);
    }

    @Transactional("businessTransactionManager")
    public void deletePage(Long id) {
        DrawingPage page = requirePage(id);
        itemRepository.deleteByDrawingPageId(page.getId());
        mediaCleanupService.scheduleCabinetImageDeletion(page.getImageUrl());
        pageRepository.delete(page);
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<DrawingCognitionItemResponse> listItems(Long pageId) {
        requirePage(pageId);
        return itemRepository.findByDrawingPageIdOrderBySortOrderAscIdAsc(pageId).stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    @Transactional("businessTransactionManager")
    public DrawingCognitionItemResponse createItem(Long pageId, CreateDrawingCognitionItemRequest request) {
        requirePage(pageId);
        validateOptionalRegion(request.getLeftPercent(), request.getTopPercent(), request.getWidthPercent(), request.getHeightPercent());
        DrawingCognitionItem item = new DrawingCognitionItem();
        item.setDrawingPageId(pageId);
        item.setTitle(request.getTitle().trim());
        item.setContent(request.getContent().trim());
        item.setLeftPercent(request.getLeftPercent());
        item.setTopPercent(request.getTopPercent());
        item.setWidthPercent(request.getWidthPercent());
        item.setHeightPercent(request.getHeightPercent());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled() == null || request.getEnabled());
        item.setCreatedAt(Instant.now());
        return toItemResponse(itemRepository.save(item));
    }

    @Transactional("businessTransactionManager")
    public DrawingCognitionItemResponse updateItem(Long id, UpdateDrawingCognitionItemRequest request) {
        DrawingCognitionItem item = requireItem(id);
        validateOptionalRegion(request.getLeftPercent(), request.getTopPercent(), request.getWidthPercent(), request.getHeightPercent());
        item.setTitle(request.getTitle().trim());
        item.setContent(request.getContent().trim());
        item.setLeftPercent(request.getLeftPercent());
        item.setTopPercent(request.getTopPercent());
        item.setWidthPercent(request.getWidthPercent());
        item.setHeightPercent(request.getHeightPercent());
        item.setSortOrder(request.getSortOrder());
        item.setEnabled(request.getEnabled());
        return toItemResponse(itemRepository.save(item));
    }

    @Transactional("businessTransactionManager")
    public void deleteItem(Long id) {
        itemRepository.delete(requireItem(id));
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<DrawingGroupSummaryResponse> listEnabledGroupSummaries(Long cabinetId) {
        screenCabinetLookupService.requireCabinet(cabinetId);
        return groupRepository.findByScreenCabinetIdAndEnabledTrueOrderByDrawingTypeAscSortOrderAscIdAsc(cabinetId).stream()
                .map(this::toLearnerGroupSummary)
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public DrawingGroupDetailResponse getEnabledGroupDetail(Long groupId) {
        DrawingGroup group = requireGroup(groupId);
        if (!Boolean.TRUE.equals(group.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "图纸分组不存在");
        }
        List<DrawingPageResponse> pages = pageRepository.findByDrawingGroupIdAndEnabledTrueOrderBySortOrderAscIdAsc(groupId).stream()
                .map(page -> new DrawingPageResponse(
                        page.getId(),
                        page.getDrawingGroupId(),
                        page.getTitle(),
                        page.getSortOrder(),
                        itemRepository.findByDrawingPageIdAndEnabledTrueOrderBySortOrderAscIdAsc(page.getId()).stream()
                                .map(this::toItemResponse)
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());
        return new DrawingGroupDetailResponse(
                group.getId(),
                group.getScreenCabinetId(),
                group.getDrawingType(),
                group.getName(),
                group.getSortOrder(),
                pages);
    }

    private DrawingGroup requireGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图纸分组不存在"));
    }

    private DrawingPage requirePage(Long id) {
        return pageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图纸不存在"));
    }

    private DrawingCognitionItem requireItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "图纸认知条目不存在"));
    }

    private void applyImageData(DrawingPage page, Long imageId, String imageUrl) {
        if (imageId != null) {
            TemporaryImage tempImage = temporaryImageRepository.findById(imageId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "临时图片不存在或已过期"));
            page.setImageData(tempImage.getImageData());
            page.setImageContentType(tempImage.getContentType());
            page.setImageUrl(null);
            temporaryImageRepository.deleteById(imageId);
        } else if (StringUtils.hasText(imageUrl)) {
            page.setImageUrl(normalizeImageUrl(imageUrl));
            page.setImageData(null);
            page.setImageContentType(null);
        } else if (hasExistingImage(page)) {
            return;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传图纸图片");
        }
    }

    private boolean hasExistingImage(DrawingPage page) {
        return StringUtils.hasText(page.getImageUrl())
                || (page.getImageData() != null && page.getImageData().length > 0);
    }

    private String normalizeImageUrl(String imageUrl) {
        String trimmed = imageUrl.trim();
        if (!trimmed.startsWith("/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片地址格式不正确");
        }
        return trimmed;
    }

    private void validateOptionalRegion(Double left, Double top, Double width, Double height) {
        if (left == null && top == null && width == null && height == null) {
            return;
        }
        if (left == null || top == null || width == null || height == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "高亮区域坐标不完整");
        }
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

    private DrawingGroupAdminResponse toAdminGroupResponse(DrawingGroup group) {
        List<DrawingPage> pages = pageRepository.findByDrawingGroupIdOrderBySortOrderAscIdAsc(group.getId());
        List<Long> pageIds = pages.stream().map(DrawingPage::getId).collect(Collectors.toList());
        long itemCount = pageIds.isEmpty() ? 0 : itemRepository.countByDrawingPageIdInAndEnabledTrue(pageIds);
        return new DrawingGroupAdminResponse(
                group.getId(),
                group.getScreenCabinetId(),
                screenCabinetLookupService.getCabinetName(group.getScreenCabinetId()),
                group.getDrawingType(),
                group.getName(),
                group.getSortOrder(),
                group.getEnabled(),
                pages.size(),
                itemCount,
                group.getCreatedAt());
    }

    private DrawingPageAdminResponse toAdminPageResponse(DrawingPage page) {
        DrawingGroup group = requireGroup(page.getDrawingGroupId());
        return new DrawingPageAdminResponse(
                page.getId(),
                page.getDrawingGroupId(),
                group.getName(),
                group.getDrawingType(),
                page.getTitle(),
                page.getImageUrl(),
                page.getSortOrder(),
                page.getEnabled(),
                itemRepository.findByDrawingPageIdOrderBySortOrderAscIdAsc(page.getId()).size(),
                page.getCreatedAt());
    }

    private DrawingCognitionItemResponse toItemResponse(DrawingCognitionItem item) {
        return new DrawingCognitionItemResponse(
                item.getId(),
                item.getDrawingPageId(),
                item.getTitle(),
                item.getContent(),
                item.getLeftPercent(),
                item.getTopPercent(),
                item.getWidthPercent(),
                item.getHeightPercent(),
                item.getSortOrder(),
                item.getEnabled(),
                item.getCreatedAt());
    }

    private DrawingGroupSummaryResponse toLearnerGroupSummary(DrawingGroup group) {
        List<DrawingPage> pages = pageRepository.findByDrawingGroupIdAndEnabledTrueOrderBySortOrderAscIdAsc(group.getId());
        List<Long> pageIds = pages.stream().map(DrawingPage::getId).collect(Collectors.toList());
        long itemCount = pageIds.isEmpty() ? 0 : itemRepository.countByDrawingPageIdInAndEnabledTrue(pageIds);
        return new DrawingGroupSummaryResponse(
                group.getId(),
                group.getScreenCabinetId(),
                group.getDrawingType(),
                group.getName(),
                group.getSortOrder(),
                pages.size(),
                itemCount);
    }
}
