package com.zeta.screen.cabinet;

import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.cabinet.CabinetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 屏柜库屏柜只读查询，供业务层校验跨库引用。
 */
@Service
public class ScreenCabinetLookupService {

    private final CabinetRepository cabinetRepository;

    public ScreenCabinetLookupService(CabinetRepository cabinetRepository) {
        this.cabinetRepository = cabinetRepository;
    }

    @Transactional(value = "screenTransactionManager", readOnly = true)
    public Cabinet requireCabinet(Long screenCabinetId) {
        return cabinetRepository.findById(screenCabinetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜不存在"));
    }

    @Transactional(value = "screenTransactionManager", readOnly = true)
    public String getCabinetName(Long screenCabinetId) {
        return requireCabinet(screenCabinetId).getName();
    }
}
