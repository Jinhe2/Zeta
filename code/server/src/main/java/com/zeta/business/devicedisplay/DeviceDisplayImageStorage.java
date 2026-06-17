package com.zeta.business.devicedisplay;

import com.zeta.config.UploadProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DeviceDisplayImageStorage {

    private static final Set<String> ALLOWED_EXTENSIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg")));

    private final UploadProperties uploadProperties;
    private final Path storageRoot;

    public DeviceDisplayImageStorage(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
        this.storageRoot = Paths.get(uploadProperties.getStorageDir()).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择图片文件");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 JPG、PNG、GIF、WebP、SVG 图片");
        }

        String filename = UUID.randomUUID() + "." + extension;
        Path targetDir = storageRoot.resolve("device-display");
        Path targetFile = targetDir.resolve(filename).normalize();
        if (!targetFile.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法文件路径");
        }

        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片保存失败");
        }

        return uploadProperties.getPublicPathPrefix() + "/device-display/" + filename;
    }

    public void deleteIfManaged(String imageUrl) {
        if (!isManagedUrl(imageUrl)) {
            return;
        }
        Path file = resolveManagedPath(imageUrl);
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // 删除失败不影响主流程
        }
    }

    public boolean isManagedUrl(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return false;
        }
        String prefix = uploadProperties.getPublicPathPrefix() + "/device-display/";
        return imageUrl.startsWith(prefix);
    }

    private Path resolveManagedPath(String imageUrl) {
        String prefix = uploadProperties.getPublicPathPrefix() + "/device-display/";
        if (!imageUrl.startsWith(prefix)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法图片地址");
        }
        String relative = "device-display/" + imageUrl.substring(prefix.length());
        Path file = storageRoot.resolve(relative).normalize();
        if (!file.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法图片地址");
        }
        return file;
    }

    private String resolveExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法识别图片格式");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
