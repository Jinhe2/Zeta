package com.zeta.business.storage;

import com.zeta.config.UploadProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

abstract class AbstractFileStorage {

  private final Path baseDir;

  protected AbstractFileStorage(String configuredBaseDir) {
    String base = configuredBaseDir;
    if (base == null || base.trim().isEmpty()) {
      base = new UploadProperties().getStorageDir();
    }
    this.baseDir = Paths.get(base).toAbsolutePath().normalize();
  }

  protected Path baseDir() {
    return baseDir;
  }

  protected String storeUnder(String relativeDir, MultipartFile file, String filename) {
    StorageUtils.requireNotEmpty(file, "上传文件不能为空");
    Path dir = baseDir.resolve(relativeDir).normalize();
    ensureInsideBase(dir);
    StorageUtils.createDirectories(dir);
    Path target = dir.resolve(filename).normalize();
    ensureInsideBase(target);
    try {
      file.transferTo(target.toFile());
      return normalizeRelativePath(baseDir.relativize(target).toString());
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "保存上传文件失败", ex);
    }
  }

  protected Resource loadManagedResource(String path) {
    Path file = resolveManagedPath(path);
    if (!Files.isRegularFile(file)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在");
    }
    try {
      return new UrlResource(file.toUri());
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取文件失败", ex);
    }
  }

  protected boolean existsManagedPath(String path) {
    return Files.isRegularFile(resolveManagedPath(path));
  }

  protected void deleteManagedPath(String path) {
    StorageUtils.deleteIfExists(resolveManagedPath(path));
  }

  protected Path resolveManagedPath(String path) {
    String normalized = normalizeManagedPath(path);
    Path resolved = baseDir.resolve(normalized).normalize();
    ensureInsideBase(resolved);
    return resolved;
  }

  public String normalizeManagedPath(String path) {
    if (path == null || path.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件路径不能为空");
    }
    String normalized = normalizeRelativePath(path.trim());
    if (normalized.startsWith("/") || normalized.startsWith("../") || normalized.contains("/../")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件路径格式不正确");
    }
    return normalized;
  }

  private void ensureInsideBase(Path path) {
    if (!path.normalize().startsWith(baseDir)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件路径格式不正确");
    }
  }

  private String normalizeRelativePath(String path) {
    return path.replace('\\', '/');
  }
}
