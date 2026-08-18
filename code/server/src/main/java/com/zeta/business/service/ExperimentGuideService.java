package com.zeta.business.service;

import com.zeta.business.entities.cabinetdisplay.TemporaryImage;
import com.zeta.business.entities.cabinetdisplay.TemporaryImageRepository;
import com.zeta.business.entities.experimentguide.ExperimentGuideItem;
import com.zeta.business.entities.experimentguide.ExperimentGuideItemRepository;
import com.zeta.business.entities.experimentguide.ExperimentGuideType;
import com.zeta.business.entities.experimentguide.dto.CreateExperimentGuideItemRequest;
import com.zeta.business.entities.experimentguide.dto.ExperimentGuideItemAdminResponse;
import com.zeta.business.entities.experimentguide.dto.ExperimentGuideItemStudentResponse;
import com.zeta.business.entities.experimentguide.dto.UpdateExperimentGuideItemRequest;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.settinglist.dto.SettingListItemResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExperimentGuideService {

  private final ExperimentGuideItemRepository repository;
  private final SettingListTargetService targetService;
  private final SettingListService settingListService;
  private final TemporaryImageRepository temporaryImageRepository;
  private final SharedMediaCleanupService mediaCleanupService;

  public ExperimentGuideService(
      ExperimentGuideItemRepository repository,
      SettingListTargetService targetService,
      SettingListService settingListService,
      TemporaryImageRepository temporaryImageRepository,
      SharedMediaCleanupService mediaCleanupService) {
    this.repository = repository;
    this.targetService = targetService;
    this.settingListService = settingListService;
    this.temporaryImageRepository = temporaryImageRepository;
    this.mediaCleanupService = mediaCleanupService;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<ExperimentGuideItemAdminResponse> listByScope(
      SettingListScopeType scopeType, Long scopeId) {
    requireScope(scopeType, scopeId);
    return repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(scopeType, scopeId).stream()
        .map(this::toAdminResponse)
        .collect(Collectors.toList());
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<ExperimentGuideItemStudentResponse> listEnabledByScope(
      SettingListScopeType scopeType, Long scopeId) {
    requireScope(scopeType, scopeId);
    List<ExperimentGuideItem> items =
        repository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(scopeType, scopeId).stream()
            .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
            .collect(Collectors.toList());

    List<SettingListItemResponse> effectiveSettings = null;
    boolean hasSettingListItem =
        items.stream().anyMatch(item -> item.getType() == ExperimentGuideType.SETTING_LIST);
    if (hasSettingListItem) {
      effectiveSettings = settingListService.get(scopeType, scopeId).getEffectiveItems();
    }
    final List<SettingListItemResponse> settings = effectiveSettings;

    return items.stream()
        .map(item -> toStudentResponse(item, settings))
        .collect(Collectors.toList());
  }

  @Transactional("businessTransactionManager")
  public ExperimentGuideItemAdminResponse create(
      SettingListScopeType scopeType, Long scopeId, CreateExperimentGuideItemRequest request) {
    requireScope(scopeType, scopeId);
    ExperimentGuideItem item = new ExperimentGuideItem();
    item.setScopeType(scopeType);
    item.setScopeId(scopeId);
    item.setType(request.getType());
    item.setTitle(request.getTitle().trim());
    item.setContent(trimToNull(request.getContent()));
    applyImage(item, request.getImageId(), request.getImageUrl());
    validateContent(item);
    item.setSortOrder(request.getSortOrder());
    item.setEnabled(request.getEnabled() == null || request.getEnabled());
    item.setCreatedAt(Instant.now());
    return toAdminResponse(repository.save(item));
  }

  @Transactional("businessTransactionManager")
  public ExperimentGuideItemAdminResponse update(Long id, UpdateExperimentGuideItemRequest request) {
    ExperimentGuideItem item = requireItem(id);
    String previousImageUrl = item.getImageUrl();
    item.setType(request.getType());
    item.setTitle(request.getTitle().trim());
    item.setContent(trimToNull(request.getContent()));
    applyImage(item, request.getImageId(), request.getImageUrl());
    validateContent(item);
    item.setSortOrder(request.getSortOrder());
    item.setEnabled(request.getEnabled());
    ExperimentGuideItem saved = repository.save(item);
    if (!Objects.equals(saved.getImageUrl(), previousImageUrl)) {
      mediaCleanupService.scheduleDeviceImageDeletion(previousImageUrl);
    }
    return toAdminResponse(saved);
  }

  @Transactional("businessTransactionManager")
  public void delete(Long id) {
    ExperimentGuideItem item = requireItem(id);
    mediaCleanupService.scheduleDeviceImageDeletion(item.getImageUrl());
    repository.delete(item);
  }

  private void requireScope(SettingListScopeType scopeType, Long scopeId) {
    if (scopeType != SettingListScopeType.LOGIC_DIAGRAM
        && scopeType != SettingListScopeType.LOGIC_GROUP) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "实验引导仅支持基础逻辑或组合逻辑作用域");
    }
    targetService.require(scopeType, scopeId);
  }

  private ExperimentGuideItem requireItem(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "实验引导条目不存在"));
  }

  private void applyImage(ExperimentGuideItem item, Long imageId, String imageUrl) {
    if (item.getType() == ExperimentGuideType.SETTING_LIST) {
      clearImage(item);
      return;
    }
    if (imageId != null) {
      TemporaryImage tempImage =
          temporaryImageRepository
              .findById(imageId)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "临时图片不存在或已过期"));
      item.setImageData(tempImage.getImageData());
      item.setImageContentType(tempImage.getContentType());
      item.setImageUrl(null);
      temporaryImageRepository.deleteById(imageId);
    } else if (StringUtils.hasText(imageUrl)) {
      item.setImageUrl(normalizeImageUrl(imageUrl));
      item.setImageData(null);
      item.setImageContentType(null);
    } else if (!hasExistingImage(item)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传引导图片");
    }
  }

  private void clearImage(ExperimentGuideItem item) {
    item.setImageUrl(null);
    item.setImageData(null);
    item.setImageContentType(null);
  }

  private boolean hasExistingImage(ExperimentGuideItem item) {
    return StringUtils.hasText(item.getImageUrl())
        || (item.getImageData() != null && item.getImageData().length > 0);
  }

  private void validateContent(ExperimentGuideItem item) {
    if (item.getType() == ExperimentGuideType.IMAGE_TEXT && !StringUtils.hasText(item.getContent())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写文字描述");
    }
  }

  private String normalizeImageUrl(String imageUrl) {
    String trimmed = imageUrl.trim();
    if (!trimmed.startsWith("/")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片地址格式不正确");
    }
    return trimmed;
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private ExperimentGuideItemAdminResponse toAdminResponse(ExperimentGuideItem item) {
    return new ExperimentGuideItemAdminResponse(
        item.getId(),
        item.getScopeType(),
        item.getScopeId(),
        item.getType(),
        item.getTitle(),
        hasExistingImage(item),
        item.getContent(),
        item.getSortOrder(),
        Boolean.TRUE.equals(item.getEnabled()),
        item.getCreatedAt());
  }

  private ExperimentGuideItemStudentResponse toStudentResponse(
      ExperimentGuideItem item, List<SettingListItemResponse> settingItems) {
    List<SettingListItemResponse> itemSettings =
        item.getType() == ExperimentGuideType.SETTING_LIST
            ? (settingItems == null ? Collections.emptyList() : settingItems)
            : null;
    return new ExperimentGuideItemStudentResponse(
        item.getId(),
        item.getType(),
        item.getTitle(),
        item.getContent(),
        hasExistingImage(item),
        item.getSortOrder(),
        itemSettings);
  }
}
