package com.zeta.business;

import com.zeta.business.cabinetdisplay.CabinetDisplayItem;
import com.zeta.business.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.business.devicedisplay.DeviceDisplayItem;
import com.zeta.business.devicedisplay.DeviceDisplayItemRepository;
import com.zeta.config.UploadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 图片数据迁移服务 — 将文件系统中的历史图片迁移到数据库。
 * 仅在应用启动时执行一次（@PostConstruct）。
 * 迁移完成后可删除此类。
 */
@Service
@Slf4j
public class ImageMigrationService {

    @Autowired
    private CabinetDisplayItemRepository cabinetRepository;

    @Autowired
    private DeviceDisplayItemRepository deviceRepository;

    @Autowired
    private UploadProperties uploadProperties;

    @PostConstruct
    public void migrateExistingImages() {
        log.info("=== 开始迁移历史图片数据到数据库 ===");

        try {
            int cabinetCount = migrateCabinetImages();
            int deviceCount = migrateDeviceImages();

            log.info("=== 图片数据迁移完成: cabinet_display_item {} 条, device_display_item {} 条 ===",
                    cabinetCount, deviceCount);
        } catch (Exception e) {
            log.error("图片数据迁移失败", e);
        }
    }

    private int migrateCabinetImages() {
        List<CabinetDisplayItem> items = cabinetRepository.findAll();
        int migrated = 0;

        for (CabinetDisplayItem item : items) {
            if (item.getImageData() != null) {
                // 已迁移，跳过
                continue;
            }

            String imageUrl = item.getImageUrl();
            if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
                log.warn("跳过无效 imageUrl: id={}, url={}", item.getId(), imageUrl);
                continue;
            }

            try {
                Path filePath = resolveFilePath(imageUrl);
                if (!Files.exists(filePath)) {
                    log.warn("文件不存在: id={}, path={}", item.getId(), filePath);
                    continue;
                }

                byte[] data = Files.readAllBytes(filePath);
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "image/jpeg";  // 默认值
                }

                item.setImageData(data);
                item.setImageContentType(contentType);
                cabinetRepository.save(item);

                migrated++;
                if (migrated % 10 == 0) {
                    log.info("已迁移 cabinet_display_item {} 条", migrated);
                }
            } catch (IOException e) {
                log.error("迁移失败: id={}, error={}", item.getId(), e.getMessage());
            }
        }

        return migrated;
    }

    private int migrateDeviceImages() {
        List<DeviceDisplayItem> items = deviceRepository.findAll();
        int migrated = 0;

        for (DeviceDisplayItem item : items) {
            if (item.getImageData() != null) {
                // 已迁移，跳过
                continue;
            }

            String imageUrl = item.getImageUrl();
            if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
                log.warn("跳过无效 imageUrl: id={}, url={}", item.getId(), imageUrl);
                continue;
            }

            try {
                Path filePath = resolveFilePath(imageUrl);
                if (!Files.exists(filePath)) {
                    log.warn("文件不存在: id={}, path={}", item.getId(), filePath);
                    continue;
                }

                byte[] data = Files.readAllBytes(filePath);
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "image/jpeg";  // 默认值
                }

                item.setImageData(data);
                item.setImageContentType(contentType);
                deviceRepository.save(item);

                migrated++;
                if (migrated % 10 == 0) {
                    log.info("已迁移 device_display_item {} 条", migrated);
                }
            } catch (IOException e) {
                log.error("迁移失败: id={}, error={}", item.getId(), e.getMessage());
            }
        }

        return migrated;
    }

    private Path resolveFilePath(String imageUrl) {
        // imageUrl 格式: /uploads/cabinet-display/xxx.jpg
        String relative = imageUrl.substring("/uploads/".length());
        return Paths.get(uploadProperties.getStorageDir(), relative);
    }
}
