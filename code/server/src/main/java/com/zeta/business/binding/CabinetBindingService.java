package com.zeta.business.binding;

import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.cabinet.CabinetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CabinetBindingService {

    private final CabinetBindingRepository bindingRepository;
    private final CabinetRepository cabinetRepository;

    public CabinetBindingService(CabinetBindingRepository bindingRepository,
                                 CabinetRepository cabinetRepository) {
        this.bindingRepository = bindingRepository;
        this.cabinetRepository = cabinetRepository;
    }

    // ── 设备绑定查询（学员/教师端调用） ─────────────────────────────────

    @Transactional(value = "screenTransactionManager", readOnly = true)
    public BindingCheckResponse checkBinding(String bindId) {
        if (bindId == null || bindId.trim().isEmpty()) {
            return new BindingCheckResponse("UNBOUND", null, null, null, null);
        }

        Optional<CabinetBinding> binding = bindingRepository.findByBindId(bindId.trim());
        if (!binding.isPresent()) {
            return new BindingCheckResponse("UNBOUND", null, null, bindId, null);
        }

        CabinetBinding b = binding.get();
        Optional<Cabinet> cabinet = cabinetRepository.findById(b.getScreenCabinetId());
        String cabinetName = cabinet.map(Cabinet::getName).orElse("(屏柜已删除)");

        return new BindingCheckResponse("BOUND", b.getScreenCabinetId(), cabinetName,
                b.getBindId(), b.getBindLabel());
    }

    // ── 管理员：屏柜绑定清单 ────────────────────────────────────────────

    @Transactional(value = "screenTransactionManager", readOnly = true)
    public List<BindingListResponse> listAllCabinetsWithBinding() {
        List<Cabinet> cabinets = cabinetRepository.findAllByOrderByIdAsc();
        Map<Long, CabinetBinding> bindingMap = bindingRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .collect(Collectors.toMap(CabinetBinding::getScreenCabinetId, b -> b, (a, b) -> b));

        List<BindingListResponse> result = new ArrayList<>();
        for (Cabinet c : cabinets) {
            CabinetBinding b = bindingMap.get(c.getId());
            result.add(new BindingListResponse(
                    c.getId(),
                    c.getName(),
                    c.getLocation(),
                    b != null ? b.getBindId() : null,
                    b != null ? b.getBindLabel() : null,
                    b != null ? b.getCreatedAt() : null));
        }
        return result;
    }

    // ── 管理员：绑定屏柜 ────────────────────────────────────────────────

    @Transactional("businessTransactionManager")
    public BindingListResponse bindCabinet(Long cabinetId, String bindId, String bindLabel) {
        // 校验屏柜存在
        Cabinet cabinet = cabinetRepository.findById(cabinetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜不存在"));

        // 校验唯一性
        if (bindingRepository.existsByScreenCabinetId(cabinetId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该屏柜已被绑定");
        }
        if (bindingRepository.existsByBindId(bindId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "绑定 ID 已被使用");
        }
        if (bindingRepository.existsByBindLabel(bindLabel)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "绑定标签已被使用");
        }

        CabinetBinding binding = new CabinetBinding();
        binding.setScreenCabinetId(cabinetId);
        binding.setBindId(bindId);
        binding.setBindLabel(bindLabel);
        binding.setCreatedAt(Instant.now());
        bindingRepository.save(binding);

        return new BindingListResponse(cabinetId, cabinet.getName(), cabinet.getLocation(),
                bindId, bindLabel, binding.getCreatedAt());
    }

    // ── 管理员：解绑屏柜 ────────────────────────────────────────────────

    @Transactional("businessTransactionManager")
    public void unbindCabinet(Long cabinetId) {
        CabinetBinding binding = bindingRepository.findByScreenCabinetId(cabinetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "该屏柜未绑定"));
        bindingRepository.delete(binding);
    }

    // ── 管理员：强制清除绑定（释放被其他平板占用的屏柜） ────────────────

    @Transactional("businessTransactionManager")
    public void forceUnbindCabinet(Long cabinetId) {
        bindingRepository.findByScreenCabinetId(cabinetId).ifPresent(bindingRepository::delete);
    }
}
