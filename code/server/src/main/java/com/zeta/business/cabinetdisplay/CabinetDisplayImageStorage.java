package com.zeta.business.cabinetdisplay;

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
public class CabinetDisplayImageStorage {

    private static final Set<String> ALLOWED_EXTENSIONS = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg")));

    private final UploadProperties uploadProperties;
    private final Path storageRoot;

    public CabinetDisplayImageStorage(UploadProperties uploadProperties) {
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
        Path targetDir = storageRoot.resolve("cabinet-display");
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

        return uploadProperties.getPublicPathPrefix() + "/cabinet-display/" + filename;
    }

    /**
     * 数据库存储模式：保存文件到 CabinetDisplayItem 记录，返回记录 ID。
     * 注意：此方法仅保存二进制数据，不创建完整的 CabinetDisplayItem 记录。
     * 实际使用时，应在 Controller 层创建完整记录并保存。
     */
    public byte[] readImageBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择图片文件");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 JPG、PNG、GIF、WebP、SVG 图片");
        }

        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "图片读取失败");
        }
    }

    /**
     * 从文件名推断 MIME 类型。
     */
    public String resolveContentType(String filename) {
        String extension = resolveExtension(filename);
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            default:
                return "image/jpeg";
        }
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
        String prefix = uploadProperties.getPublicPathPrefix() + "/cabinet-display/";
        return imageUrl.startsWith(prefix);
    }

    private Path resolveManagedPath(String imageUrl) {
        String prefix = uploadProperties.getPublicPathPrefix() + "/cabinet-display/";
        if (!imageUrl.startsWith(prefix)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法图片地址");
        }
        String relative = "cabinet-display/" + imageUrl.substring(prefix.length());
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
