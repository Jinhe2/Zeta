package com.zeta.business.media;

import com.zeta.config.UploadProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CognitionVideoStorage {

    public static final long MAX_VIDEO_BYTES = 50L * 1024L * 1024L;
    private static final String RELATIVE_PREFIX = "resource/video/";
    private static final Pattern MANAGED_PATH = Pattern.compile("^resource/video/[0-9a-fA-F-]{36}\\.mp4$");

    private final Path jarDirectory;
    private final Path videoDirectory;

    public CognitionVideoStorage(UploadProperties uploadProperties) {
        this.jarDirectory = resolveJarDirectory(uploadProperties.getVideoStorageDir());
        this.videoDirectory = jarDirectory.resolve("resource/video").normalize();
    }

    public String store(MultipartFile file) {
        validate(file);
        String relativePath = RELATIVE_PREFIX + UUID.randomUUID() + ".mp4";
        Path target = resolve(relativePath);
        Path temporary = target.resolveSibling(target.getFileName() + ".uploading");
        try {
            Files.createDirectories(videoDirectory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return relativePath;
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // Best-effort cleanup.
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "视频保存失败");
        }
    }

    public Resource load(String relativePath) {
        Path file = resolveExisting(relativePath);
        return new FileSystemResource(file);
    }

    public boolean exists(String relativePath) {
        try {
            return Files.isRegularFile(resolve(relativePath));
        } catch (ResponseStatusException ex) {
            return false;
        }
    }

    public void delete(String relativePath) {
        if (!StringUtils.hasText(relativePath)) return;
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "视频删除失败");
        }
    }

    public void deleteAfterCommit(String relativePath) {
        if (!StringUtils.hasText(relativePath)) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delete(relativePath);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    delete(relativePath);
                } catch (ResponseStatusException ignored) {
                    // The database change is already committed; do not fail the request.
                }
            }
        });
    }

    public String normalizeManagedPath(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传认知视频");
        }
        String normalized = relativePath.trim().replace('\\', '/');
        if (!MANAGED_PATH.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "视频路径格式不正确");
        }
        resolve(normalized);
        return normalized;
    }

    private Path resolveExisting(String relativePath) {
        Path file = resolve(relativePath);
        if (!Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "视频不存在");
        }
        return file;
    }

    private Path resolve(String relativePath) {
        String normalized = normalizePathSyntax(relativePath);
        Path file = jarDirectory.resolve(normalized).normalize();
        if (!file.startsWith(videoDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法视频路径");
        }
        return file;
    }

    private String normalizePathSyntax(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "视频路径不能为空");
        }
        String normalized = relativePath.trim().replace('\\', '/');
        if (!MANAGED_PATH.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "视频路径格式不正确");
        }
        return normalized;
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择视频文件");
        }
        if (file.getSize() > MAX_VIDEO_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "视频文件不能超过 50MB");
        }
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.toLowerCase(Locale.ROOT).endsWith(".mp4")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 MP4 视频");
        }
        if (!"video/mp4".equalsIgnoreCase(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 MP4 视频");
        }
        byte[] header = new byte[12];
        try (InputStream input = file.getInputStream()) {
            int read = input.read(header);
            if (read < 12 || header[4] != 'f' || header[5] != 't' || header[6] != 'y' || header[7] != 'p') {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "视频文件格式无效");
            }
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "视频文件读取失败");
        }
    }

    private Path resolveJarDirectory(String configuredStorageDir) {
        if (StringUtils.hasText(configuredStorageDir)) {
            Path configured = Paths.get(configuredStorageDir).toAbsolutePath().normalize();
            return configured.endsWith(Paths.get("resource/video"))
                    ? configured.getParent().getParent()
                    : configured;
        }
        try {
            File source = new ApplicationHome(CognitionVideoStorage.class).getSource();
            if (source != null && source.isFile()) {
                return source.toPath().toAbsolutePath().normalize().getParent();
            }
        } catch (RuntimeException ignored) {
            // Fall back to the process working directory for IDE/dev runs.
        }
        return Paths.get("").toAbsolutePath().normalize();
    }
}
