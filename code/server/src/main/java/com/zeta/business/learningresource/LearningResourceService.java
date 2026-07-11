package com.zeta.business.learningresource;

import com.zeta.business.binding.BindingCheckResponse;
import com.zeta.business.binding.CabinetBindingService;
import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.cabinet.CabinetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LearningResourceService {
    private final LearningResourceRepository repository;
    private final CabinetRepository cabinetRepository;
    private final CabinetBindingService bindingService;
    private final LearningResourceStorage storage;

    public LearningResourceService(LearningResourceRepository repository,
                                   CabinetRepository cabinetRepository,
                                   CabinetBindingService bindingService,
                                   LearningResourceStorage storage) {
        this.repository = repository;
        this.cabinetRepository = cabinetRepository;
        this.bindingService = bindingService;
        this.storage = storage;
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<LearningResourceResponse> listAdmin(LearningResourceType type, Long cabinetId) {
        return repository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(item -> type == null || item.getResourceType() == type)
                .filter(item -> cabinetId == null || cabinetId.equals(item.getScreenCabinetId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional("businessTransactionManager")
    public LearningResourceResponse create(String name, String description, LearningResourceType type,
                                           Long cabinetId, MultipartFile file) {
        validateText(name, description);
        validateCabinet(cabinetId);
        LearningResourceStorage.StoredFile stored = storage.store(type, file);
        try {
            LearningResource item = new LearningResource();
            applyTextAndScope(item, name, description, type, cabinetId);
            applyStoredFile(item, stored);
            Instant now = Instant.now();
            item.setCreatedAt(now);
            item.setUpdatedAt(now);
            return toResponse(repository.save(item));
        } catch (RuntimeException ex) {
            storage.delete(stored.getPath());
            throw ex;
        }
    }

    @Transactional("businessTransactionManager")
    public LearningResourceResponse update(Long id, String name, String description, LearningResourceType type,
                                           Long cabinetId, MultipartFile file) {
        validateText(name, description);
        validateCabinet(cabinetId);
        LearningResource item = require(id);
        if (item.getResourceType().isPdf() != type.isPdf() && (file == null || file.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF 与视频资料之间切换时必须上传新文件");
        }
        LearningResourceStorage.StoredFile replacement = null;
        if (file != null && !file.isEmpty()) replacement = storage.store(type, file);
        String previousPath = item.getFilePath();
        try {
            applyTextAndScope(item, name, description, type, cabinetId);
            if (replacement != null) applyStoredFile(item, replacement);
            item.setUpdatedAt(Instant.now());
            LearningResourceResponse response = toResponse(repository.save(item));
            if (replacement != null && !previousPath.equals(replacement.getPath())) storage.deleteAfterCommit(previousPath);
            return response;
        } catch (RuntimeException ex) {
            if (replacement != null) storage.delete(replacement.getPath());
            throw ex;
        }
    }

    @Transactional("businessTransactionManager")
    public void delete(Long id) {
        LearningResource item = require(id);
        String path = item.getFilePath();
        repository.delete(item);
        storage.deleteAfterCommit(path);
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<LearningResourceResponse> listForBoundCabinet(LearningResourceType type, String bindId,
                                                               Long selectedCabinetId, boolean allowSelectedCabinet) {
        Long cabinetId = resolveCabinet(bindId, selectedCabinetId, allowSelectedCabinet);
        return repository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(item -> item.getResourceType() == type)
                .filter(item -> item.getScreenCabinetId() == null || cabinetId.equals(item.getScreenCabinetId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public LearningResourceResponse getForBoundCabinet(Long id, String bindId, Long selectedCabinetId,
                                                        boolean allowSelectedCabinet) {
        return toResponse(requireAccessible(id, bindId, selectedCabinetId, allowSelectedCabinet));
    }

    @Transactional(value = "businessTransactionManager", readOnly = true)
    public LearningResource getFileForBoundCabinet(Long id, String bindId, Long selectedCabinetId,
                                                    boolean allowSelectedCabinet) {
        return requireAccessible(id, bindId, selectedCabinetId, allowSelectedCabinet);
    }

    private LearningResource requireAccessible(Long id, String bindId, Long selectedCabinetId,
                                               boolean allowSelectedCabinet) {
        Long cabinetId = resolveCabinet(bindId, selectedCabinetId, allowSelectedCabinet);
        LearningResource item = require(id);
        if (item.getScreenCabinetId() != null && !cabinetId.equals(item.getScreenCabinetId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学习资料不存在");
        }
        return item;
    }

    private Long resolveCabinet(String bindId, Long selectedCabinetId, boolean allowSelectedCabinet) {
        if (allowSelectedCabinet && selectedCabinetId != null) {
            if (!cabinetRepository.existsById(selectedCabinetId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "指定屏柜不存在");
            }
            return selectedCabinetId;
        }
        BindingCheckResponse binding = bindingService.checkBinding(bindId);
        if ("BOUND".equals(binding.getStatus()) && binding.getCabinetId() != null) {
            return binding.getCabinetId();
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前设备未绑定屏柜");
    }

    private LearningResource require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "学习资料不存在"));
    }

    private void validateText(String name, String description) {
        if (!StringUtils.hasText(name) || name.trim().length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "资料名称不能为空且不能超过 128 个字符");
        }
        if (!StringUtils.hasText(description)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "资料说明不能为空");
        }
    }

    private void validateCabinet(Long cabinetId) {
        if (cabinetId != null && !cabinetRepository.existsById(cabinetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "指定屏柜不存在");
        }
    }

    private void applyTextAndScope(LearningResource item, String name, String description,
                                   LearningResourceType type, Long cabinetId) {
        item.setName(name.trim());
        item.setDescription(description.trim());
        item.setResourceType(type);
        item.setScreenCabinetId(cabinetId);
    }

    private void applyStoredFile(LearningResource item, LearningResourceStorage.StoredFile file) {
        item.setFilePath(file.getPath());
        item.setOriginalFilename(file.getOriginalFilename());
        item.setContentType(file.getContentType());
        item.setFileSize(file.getSize());
    }

    private LearningResourceResponse toResponse(LearningResource item) {
        String cabinetName = null;
        if (item.getScreenCabinetId() != null) {
            cabinetName = cabinetRepository.findById(item.getScreenCabinetId())
                    .map(Cabinet::getName).orElse("(屏柜已删除)");
        }
        return new LearningResourceResponse(item.getId(), item.getName(), item.getDescription(),
                item.getResourceType(), item.getScreenCabinetId(), cabinetName,
                item.getOriginalFilename(), item.getContentType(), item.getFileSize(),
                item.getCreatedAt(), item.getUpdatedAt());
    }
}
