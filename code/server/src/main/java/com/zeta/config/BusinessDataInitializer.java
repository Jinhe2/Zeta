package com.zeta.config;

import com.zeta.business.cabinetdisplay.CabinetDisplayItem;
import com.zeta.business.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.business.cognitiondevice.CognitionDevice;
import com.zeta.business.cognitiondevice.CognitionDeviceRepository;
import com.zeta.business.cognitiondevice.CognitionDeviceType;
import com.zeta.business.devicedisplay.DeviceDisplayItem;
import com.zeta.business.devicedisplay.DeviceDisplayItemRepository;
import com.zeta.business.user.User;
import com.zeta.business.user.UserRepository;
import com.zeta.business.user.UserRole;
import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.cabinet.CabinetRepository;
import com.zeta.screen.ieddevice.Device;
import com.zeta.screen.ieddevice.DeviceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

/**
 * 业务库 ct-screen-monitor 种子数据：用户、屏柜/设备展示条目等。
 */
@Component
@Order(20)
@ConditionalOnProperty(name = "zeta.seed.enabled", havingValue = "true", matchIfMissing = true)
public class BusinessDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CabinetDisplayItemRepository cabinetDisplayItemRepository;
    private final CognitionDeviceRepository cognitionDeviceRepository;
    private final DeviceDisplayItemRepository deviceDisplayItemRepository;
    private final CabinetRepository cabinetRepository;
    private final DeviceRepository deviceRepository;

    public BusinessDataInitializer(
            UserRepository userRepository,
            CabinetDisplayItemRepository cabinetDisplayItemRepository,
            CognitionDeviceRepository cognitionDeviceRepository,
            DeviceDisplayItemRepository deviceDisplayItemRepository,
            CabinetRepository cabinetRepository,
            DeviceRepository deviceRepository) {
        this.userRepository = userRepository;
        this.cabinetDisplayItemRepository = cabinetDisplayItemRepository;
        this.cognitionDeviceRepository = cognitionDeviceRepository;
        this.deviceDisplayItemRepository = deviceDisplayItemRepository;
        this.cabinetRepository = cabinetRepository;
        this.deviceRepository = deviceRepository;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedCabinetDisplayItemsIfPresent();
        seedDeviceDisplayItemsIfPresent();
    }

    @Transactional("businessTransactionManager")
    public void seedUsers() {
        createUserIfAbsent("student", "123456", "学员", UserRole.STUDENT);
        createUserIfAbsent("teacher", "123456", "教师", UserRole.TEACHER);
        createUserIfAbsent("admin", "123456", "管理员", UserRole.ADMIN);
    }

    private void seedCabinetDisplayItemsIfPresent() {
        Optional<Cabinet> cabinet = cabinetRepository.findByLocation("cabinet-line-220");
        if (!cabinet.isPresent()) {
            cabinet = cabinetRepository.findByName("220kV 线路保护屏");
        }
        cabinet.ifPresent(this::seedCabinetDisplayItems);
    }

    private void seedDeviceDisplayItemsIfPresent() {
        Optional<Device> deviceA = deviceRepository.findByIedName("device-line-a");
        deviceA.ifPresent(this::seedDeviceDisplayItems);
    }

    @Transactional("businessTransactionManager")
    public void seedCabinetDisplayItems(Cabinet cabinet) {
        upsertCabinetDisplayItem(cabinet.getId(), "正视图",
                "/images/cabinet-structure.svg",
                "220kV 线路保护屏为金属封闭式屏柜，自上而下通常划分为交流电压/电流回路区、保护装置安装区、端子排及出线区。",
                0);
        upsertCabinetDisplayItem(cabinet.getId(), "侧视图",
                "/images/cabinet-structure.svg",
                "屏内二次回路按功能分区布置：交流采样、开入开出、通信及电源回路应能清晰辨识，便于运维与故障排查。",
                1);
        upsertCabinetDisplayItem(cabinet.getId(), "后视图",
                "/images/cabinet-structure.svg",
                "认知屏柜时应了解屏门联锁、接地要求及带电检修安全距离，明确装置、端子排与外部设备的对应关系。",
                2);
    }

    @Transactional("businessTransactionManager")
    public void seedDeviceDisplayItems(Device device) {
        CabinetDisplayItem frontView = cabinetDisplayItemRepository
                .findByScreenCabinetIdAndTitle(device.getCabinet().getId(), "正视图")
                .orElse(null);
        if (frontView == null) {
            return;
        }
        CognitionDevice cognitionDevice = upsertIedCognitionDevice(
                frontView.getId(), device.getId(), device.getIedName(), 30.0, 25.0, 20.0, 15.0, 0);

        upsertDeviceDisplayItem(cognitionDevice.getId(), "装置面板与指示灯",
                "屏柜内核心设备为继电保护测控装置，通常面板安装，正面包含液晶显示区、运行/动作/告警指示灯及本地操作按键。",
                0);
        upsertDeviceDisplayItem(cognitionDevice.getId(), "液晶界面与自检",
                "通过液晶界面可查看模拟量、开关量状态、定值区信息及装置自检结果；指示灯用于快速判断装置运行与故障情况。",
                1);
        upsertDeviceDisplayItem(cognitionDevice.getId(), "接线端子与屏内连接",
                "装置背面或侧方设有交流电压、电流、开入开出等接线端子，通过屏柜内电缆与端子排、断路器辅助接点相连。",
                2);
        upsertDeviceDisplayItem(cognitionDevice.getId(), "型号参数与安装检修",
                "学习设备认知时，应能识别装置型号、额定参数、通信接口位置，并理解装置在屏柜中的安装层级与检修空间要求。",
                3);
    }

    private void createUserIfAbsent(String username, String password, String displayName, UserRole role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setCreatedAt(Instant.now());
        userRepository.save(user);
    }

    private void upsertCabinetDisplayItem(
            Long screenCabinetId, String title, String imageUrl, String content, int sortOrder) {
        CabinetDisplayItem item = cabinetDisplayItemRepository
                .findByScreenCabinetIdAndTitle(screenCabinetId, title)
                .orElseGet(CabinetDisplayItem::new);
        item.setScreenCabinetId(screenCabinetId);
        item.setTitle(title);
        item.setImageUrl(imageUrl);
        item.setContent(content);
        item.setSortOrder(sortOrder);
        item.setEnabled(true);
        if (item.getCreatedAt() == null) {
            item.setCreatedAt(Instant.now());
        }
        cabinetDisplayItemRepository.save(item);
    }

    private CognitionDevice upsertIedCognitionDevice(
            Long cabinetDisplayItemId,
            Long screenDeviceId,
            String title,
            double left,
            double top,
            double width,
            double height,
            int sortOrder) {
        CognitionDevice device = cognitionDeviceRepository
                .findByScreenDeviceIdAndCabinetDisplayItemId(screenDeviceId, cabinetDisplayItemId)
                .orElseGet(CognitionDevice::new);
        device.setCabinetDisplayItemId(cabinetDisplayItemId);
        device.setDeviceType(CognitionDeviceType.IED);
        device.setScreenDeviceId(screenDeviceId);
        device.setTitle(title);
        device.setLeftPercent(left);
        device.setTopPercent(top);
        device.setWidthPercent(width);
        device.setHeightPercent(height);
        device.setSortOrder(sortOrder);
        device.setEnabled(true);
        if (device.getCreatedAt() == null) {
            device.setCreatedAt(Instant.now());
        }
        return cognitionDeviceRepository.save(device);
    }

    private void upsertDeviceDisplayItem(Long cognitionDeviceId, String title, String content, int sortOrder) {
        DeviceDisplayItem item = deviceDisplayItemRepository
                .findByCognitionDeviceIdAndTitle(cognitionDeviceId, title)
                .orElseGet(DeviceDisplayItem::new);
        item.setCognitionDeviceId(cognitionDeviceId);
        item.setTitle(title);
        if (!StringUtils.hasText(item.getImageUrl())) {
            item.setImageUrl("/images/protection-device.svg");
        }
        item.setContent(content);
        item.setSortOrder(sortOrder);
        item.setEnabled(true);
        if (item.getCreatedAt() == null) {
            item.setCreatedAt(Instant.now());
        }
        deviceDisplayItemRepository.save(item);
    }
}
