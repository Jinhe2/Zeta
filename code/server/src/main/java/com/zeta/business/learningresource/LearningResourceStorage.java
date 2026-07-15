package com.zeta.business.learningresource;

import com.zeta.config.UploadProperties;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class LearningResourceStorage {
    public static final long MAX_PDF_BYTES = 50L * 1024L * 1024L;
    public static final long MAX_VIDEO_BYTES = 500L * 1024L * 1024L;
    private static final Pattern PDF_PATH = Pattern.compile("^resource/pdf/[0-9a-fA-F-]{36}\\.pdf$");
    private static final Pattern VIDEO_PATH = Pattern.compile("^resource/video/[0-9a-fA-F-]{36}\\.mp4$");

    private final Path rootDirectory;

    public LearningResourceStorage(UploadProperties properties) {
        this.rootDirectory = resolveRootDirectory(properties.getResourceStorageDir());
    }

    public StoredFile store(LearningResourceType type, MultipartFile file) {
        validate(type, file);
        String extension = type.isPdf() ? ".pdf" : ".mp4";
        String relativePath = (type.isPdf() ? "resource/pdf/" : "resource/video/") + UUID.randomUUID() + extension;
        Path target = resolve(relativePath);
        Path temporary = target.resolveSibling(target.getFileName() + ".uploading");
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(relativePath, file.getOriginalFilename().trim(),
                    type.isPdf() ? "application/pdf" : "video/mp4", file.getSize());
        } catch (IOException ex) {
            deleteQuietly(temporary);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "学习资料保存失败");
        }
    }

    public Resource load(String path) {
        Path file = resolve(path);
        if (!Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学习资料文件不存在");
        }
        return new FileSystemResource(file);
    }

    public void deleteAfterCommit(String path) {
        if (!StringUtils.hasText(path)) return;
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            delete(path);
            return;
        }
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override public void afterCommit() { deleteQuietly(resolve(path)); }
                });
    }

    public void delete(String path) {
        try {
            Files.deleteIfExists(resolve(path));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "学习资料文件删除失败");
        }
    }

    private void validate(LearningResourceType type, MultipartFile file) {
        if (type == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "资料分类不能为空");
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择学习资料文件");
        String filename = file.getOriginalFilename();
        if (!StringUtils.hasText(filename)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件名无效");
        String lower = filename.toLowerCase(Locale.ROOT);
        long max = type.isPdf() ? MAX_PDF_BYTES : MAX_VIDEO_BYTES;
        if (file.getSize() > max) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                type.isPdf() ? "PDF 文件不能超过 50MB" : "视频文件不能超过 500MB");
        if (type.isPdf()) {
            if (!lower.endsWith(".pdf") || !"application/pdf".equalsIgnoreCase(file.getContentType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 PDF 文件");
            }
            verifyPdfHeader(file);
        } else {
            if (!lower.endsWith(".mp4") || !"video/mp4".equalsIgnoreCase(file.getContentType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 MP4 视频");
            }
            verifyMp4Header(file);
        }
    }

    private void verifyPdfHeader(MultipartFile file) {
        byte[] header = readHeader(file, 5);
        if (header.length < 5 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F' || header[4] != '-') {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PDF 文件格式无效");
        }
    }

    private void verifyMp4Header(MultipartFile file) {
        byte[] header = readHeader(file, 12);
        if (header.length < 12 || header[4] != 'f' || header[5] != 't' || header[6] != 'y' || header[7] != 'p') {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "视频文件格式无效");
        }
    }

    private byte[] readHeader(MultipartFile file, int size) {
        byte[] bytes = new byte[size];
        try (InputStream input = file.getInputStream()) {
            int read = input.read(bytes);
            if (read < 0) return new byte[0];
            if (read == size) return bytes;
            byte[] result = new byte[read];
            System.arraycopy(bytes, 0, result, 0, read);
            return result;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习资料文件读取失败");
        }
    }

    private Path resolve(String relativePath) {
        String normalized = relativePath == null ? "" : relativePath.trim().replace('\\', '/');
        if (!PDF_PATH.matcher(normalized).matches() && !VIDEO_PATH.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "学习资料路径格式不正确");
        }
        Path path = rootDirectory.resolve(normalized).normalize();
        if (!path.startsWith(rootDirectory.resolve("resource").normalize())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法学习资料路径");
        }
        return path;
    }

    private Path resolveRootDirectory(String configured) {
        if (StringUtils.hasText(configured)) return Paths.get(configured).toAbsolutePath().normalize();
        try {
            File source = new ApplicationHome(LearningResourceStorage.class).getSource();
            if (source != null && source.isFile()) return source.toPath().toAbsolutePath().normalize().getParent();
        } catch (RuntimeException ignored) { }
        return Paths.get("").toAbsolutePath().normalize();
    }

    private void deleteQuietly(Path file) {
        try { Files.deleteIfExists(file); } catch (IOException ignored) { }
    }

    public static class StoredFile {
        private final String path;
        private final String originalFilename;
        private final String contentType;
        private final long size;
        StoredFile(String path, String originalFilename, String contentType, long size) {
            this.path = path; this.originalFilename = originalFilename; this.contentType = contentType; this.size = size;
        }
        public String getPath() { return path; }
        public String getOriginalFilename() { return originalFilename; }
        public String getContentType() { return contentType; }
        public long getSize() { return size; }
    }
}
