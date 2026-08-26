package com.zeta.business.storage;

import com.zeta.config.UploadProperties;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

abstract class AbstractImageStorage {

  private static final Set<String> SUPPORTED_EXTENSIONS =
      new HashSet<String>(Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "svg"));
  private static final long MAX_IMAGE_BYTES = 20L * 1024L * 1024L;

  private final Path storageDir;
  private final String publicPathPrefix;

  protected AbstractImageStorage(UploadProperties properties) {
    this.storageDir = Paths.get(properties.getStorageDir()).toAbsolutePath().normalize();
    this.publicPathPrefix = properties.getPublicPathPrefix();
  }

  public byte[] readImageBytes(MultipartFile file) {
    StorageUtils.requireNotEmpty(file, "图片不能为空");
    StorageUtils.requireMaxSize(file, MAX_IMAGE_BYTES, "图片");
    resolveContentType(file.getOriginalFilename());
    try {
      return file.getBytes();
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取图片失败", ex);
    }
  }

  public String resolveContentType(String filename) {
    String extension = StorageUtils.extensionOf(filename);
    if (!SUPPORTED_EXTENSIONS.contains(extension)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 jpg、png、gif、webp、svg 图片");
    }
    if ("jpg".equals(extension) || "jpeg".equals(extension)) {
      return "image/jpeg";
    }
    if ("svg".equals(extension)) {
      return "image/svg+xml";
    }
    return "image/" + extension;
  }

  public void deleteIfManaged(String imageUrl) {
    if (!StringUtils.hasText(imageUrl)) {
      return;
    }
    String prefix = publicPathPrefix.endsWith("/") ? publicPathPrefix : publicPathPrefix + "/";
    if (!imageUrl.startsWith(prefix)) {
      return;
    }
    String relativePath = imageUrl.substring(prefix.length());
    Path target = storageDir.resolve(relativePath).normalize();
    if (!target.startsWith(storageDir)) {
      return;
    }
    StorageUtils.deleteAfterCommit(
        new Runnable() {
          @Override
          public void run() {
            StorageUtils.deleteIfExists(target);
          }
        });
  }
}
