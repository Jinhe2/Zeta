package com.zeta.config;

import com.zeta.domain.Cabinet;
import com.zeta.domain.Device;
import com.zeta.domain.DeviceCognitionItem;
import com.zeta.domain.ProtectionLogic;
import com.zeta.domain.User;
import com.zeta.domain.UserRole;
import com.zeta.repository.CabinetRepository;
import com.zeta.repository.DeviceCognitionItemRepository;
import com.zeta.repository.DeviceRepository;
import com.zeta.repository.ProtectionLogicRepository;
import com.zeta.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CabinetRepository cabinetRepository;
    private final DeviceRepository deviceRepository;
    private final ProtectionLogicRepository protectionLogicRepository;
    private final DeviceCognitionItemRepository cognitionItemRepository;

    public DataInitializer(
            UserRepository userRepository,
            CabinetRepository cabinetRepository,
            DeviceRepository deviceRepository,
            ProtectionLogicRepository protectionLogicRepository,
            DeviceCognitionItemRepository cognitionItemRepository) {
        this.userRepository = userRepository;
        this.cabinetRepository = cabinetRepository;
        this.deviceRepository = deviceRepository;
        this.protectionLogicRepository = protectionLogicRepository;
        this.cognitionItemRepository = cognitionItemRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUsers();
        seedKnowledgeStructure();
    }

    private void seedUsers() {
        createUserIfAbsent("student", "123456", "学员", UserRole.STUDENT);
        createUserIfAbsent("teacher", "123456", "教师", UserRole.TEACHER);
        createUserIfAbsent("admin", "123456", "管理员", UserRole.ADMIN);
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

    private void seedKnowledgeStructure() throws Exception {
        Cabinet cabinet = upsertCabinet(
                "cabinet-line-220",
                "220kV 线路保护屏",
                "220kV 线路继电保护屏柜，集中安装线路保护及相关二次设备。",
                0);

        Device deviceA = upsertDevice(
                cabinet,
                "device-line-a",
                "线路保护装置 A",
                "主保护装置，含过流、重合闸等保护逻辑。",
                0);
        Device deviceB = upsertDevice(
                cabinet,
                "device-line-b",
                "线路保护装置 B",
                "辅助保护装置，含差动等保护逻辑。",
                1);

        upsertLogic(deviceA, "example", "过流 I 段保护逻辑",
                "过流 I 段保护控制逻辑框图，含方向、电压、时间元件等典型结构。",
                "过流保护", 0, "samples/example.json");
        upsertLogic(deviceA, "reclose", "重合闸保护逻辑",
                "重合闸投入条件、检同期/检无压及 TWJ 启动等逻辑。",
                "重合闸", 1, "samples/reclose.json");
        upsertLogic(deviceB, "diff", "差动保护逻辑",
                "本侧差动允许条件，含光纤同步、电流启动与电压辅助等输入。",
                "差动保护", 0, "samples/1.json");

        migrateOrphanLogics(deviceA, deviceB);
        seedCognitionItems(deviceA);
    }

    private void seedCognitionItems(Device device) {
        upsertCognitionItem(device, "装置面板与指示灯",
                "屏柜内核心设备为继电保护测控装置，通常面板安装，正面包含液晶显示区、运行/动作/告警指示灯及本地操作按键。",
                0);
        upsertCognitionItem(device, "液晶界面与自检",
                "通过液晶界面可查看模拟量、开关量状态、定值区信息及装置自检结果；指示灯用于快速判断装置运行与故障情况。",
                1);
        upsertCognitionItem(device, "接线端子与屏内连接",
                "装置背面或侧方设有交流电压、电流、开入开出等接线端子，通过屏柜内电缆与端子排、断路器辅助接点相连。",
                2);
        upsertCognitionItem(device, "型号参数与安装检修",
                "学习设备认知时，应能识别装置型号、额定参数、通信接口位置，并理解装置在屏柜中的安装层级与检修空间要求。",
                3);
    }

    private void upsertCognitionItem(Device device, String title, String content, int sortOrder) {
        DeviceCognitionItem item = cognitionItemRepository
                .findByDeviceIdAndTitle(device.getId(), title)
                .orElseGet(DeviceCognitionItem::new);
        item.setDevice(device);
        item.setTitle(title);
        item.setContent(content);
        item.setSortOrder(sortOrder);
        item.setEnabled(true);
        if (item.getCreatedAt() == null) {
            item.setCreatedAt(Instant.now());
        }
        cognitionItemRepository.save(item);
    }

    /** 将历史数据中未关联设备的保护逻辑挂到默认设备 */
    private void migrateOrphanLogics(Device defaultDevice, Device secondaryDevice) {
        for (ProtectionLogic logic : protectionLogicRepository.findByDeviceIsNull()) {
            switch (logic.getCode()) {
                case "diff":
                    logic.setDevice(secondaryDevice);
                    break;
                default:
                    logic.setDevice(defaultDevice);
                    break;
            }
            protectionLogicRepository.save(logic);
        }
    }

    private Cabinet upsertCabinet(String code, String name, String description, int sortOrder) {
        Cabinet cabinet = cabinetRepository.findByCode(code).orElseGet(Cabinet::new);
        cabinet.setCode(code);
        cabinet.setName(name);
        cabinet.setDescription(description);
        cabinet.setSortOrder(sortOrder);
        cabinet.setEnabled(true);
        if (cabinet.getCreatedAt() == null) {
            cabinet.setCreatedAt(Instant.now());
        }
        return cabinetRepository.save(cabinet);
    }

    private Device upsertDevice(Cabinet cabinet, String code, String name, String description, int sortOrder) {
        Device device = deviceRepository.findByCode(code).orElseGet(Device::new);
        device.setCabinet(cabinet);
        device.setCode(code);
        device.setName(name);
        device.setDescription(description);
        device.setSortOrder(sortOrder);
        device.setEnabled(true);
        if (device.getCreatedAt() == null) {
            device.setCreatedAt(Instant.now());
        }
        return deviceRepository.save(device);
    }

    private void upsertLogic(
            Device device,
            String code,
            String title,
            String description,
            String category,
            int sortOrder,
            String resourcePath) throws Exception {
        ProtectionLogic logic = protectionLogicRepository.findByCode(code).orElseGet(ProtectionLogic::new);
        logic.setDevice(device);
        logic.setCode(code);
        logic.setTitle(title);
        logic.setDescription(description);
        logic.setCategory(category);
        logic.setSortOrder(sortOrder);
        logic.setConfigJson(readResource(resourcePath));
        logic.setEnabled(true);
        if (logic.getCreatedAt() == null) {
            logic.setCreatedAt(Instant.now());
        }
        protectionLogicRepository.save(logic);
    }

    private String readResource(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }
}
