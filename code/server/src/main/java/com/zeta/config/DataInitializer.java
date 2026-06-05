package com.zeta.config;

import com.zeta.domain.ProtectionLogic;
import com.zeta.domain.User;
import com.zeta.domain.UserRole;
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
    private final ProtectionLogicRepository protectionLogicRepository;

    public DataInitializer(UserRepository userRepository, ProtectionLogicRepository protectionLogicRepository) {
        this.userRepository = userRepository;
        this.protectionLogicRepository = protectionLogicRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUsers();
        seedProtectionLogics();
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

    private void seedProtectionLogics() throws Exception {
        upsertLogic("example", "过流 I 段保护逻辑",
                "过流 I 段保护控制逻辑框图，含方向、电压、时间元件等典型结构。",
                "过流保护", "samples/example.json");
        upsertLogic("reclose", "重合闸保护逻辑",
                "重合闸投入条件、检同期/检无压及 TWJ 启动等逻辑。",
                "重合闸", "samples/reclose.json");
        upsertLogic("diff", "差动保护逻辑",
                "本侧差动允许条件，含光纤同步、电流启动与电压辅助等输入。",
                "差动保护", "samples/1.json");
    }

    private void upsertLogic(String code, String title, String description, String category, String resourcePath)
            throws Exception {
        ProtectionLogic logic = protectionLogicRepository.findByCode(code).orElseGet(ProtectionLogic::new);
        logic.setCode(code);
        logic.setTitle(title);
        logic.setDescription(description);
        logic.setCategory(category);
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
