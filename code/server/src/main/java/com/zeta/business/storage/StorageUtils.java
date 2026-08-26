package com.zeta.business.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

final class StorageUtils {

  private StorageUtils() {}

  static void requireNotEmpty(MultipartFile file, String message) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
  }

  static void requireMaxSize(MultipartFile file, long maxBytes, String label) {
    if (file != null && file.getSize() > maxBytes) {
      throw new ResponseStatusException(
          HttpStatus.PAYLOAD_TOO_LARGE,
          label + "不能超过 " + formatSize(maxBytes) + "，当前约 " + formatSize(file.getSize()));
    }
  }

  private static String formatSize(long bytes) {
    long mb = 1024L * 1024L;
    if (bytes >= mb) {
      long value = (bytes + mb - 1) / mb;
      return value + "MB";
    }
    long kb = 1024L;
    long value = (bytes + kb - 1) / kb;
    return value + "KB";
  }

  static String cleanFilename(String originalFilename) {
    if (!StringUtils.hasText(originalFilename)) {
      return "upload";
    }
    String normalized = originalFilename.replace('\\', '/');
    int slash = normalized.lastIndexOf('/');
    String filename = slash >= 0 ? normalized.substring(slash + 1) : normalized;
    return StringUtils.hasText(filename) ? filename : "upload";
  }

  static String extensionOf(String originalFilename) {
    String filename = cleanFilename(originalFilename);
    int dot = filename.lastIndexOf('.');
    if (dot < 0 || dot == filename.length() - 1) {
      return "";
    }
    return filename.substring(dot + 1).toLowerCase();
  }

  static void createDirectories(Path dir) {
    try {
      Files.createDirectories(dir);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "创建上传目录失败", ex);
    }
  }

  static void deleteIfExists(Path path) {
    if (path == null) {
      return;
    }
    try {
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "删除文件失败", ex);
    }
  }

  static void deleteAfterCommit(final Runnable deleteAction) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              deleteAction.run();
            }
          });
    } else {
      deleteAction.run();
    }
  }
}
