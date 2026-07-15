package com.zeta.business.media;

import com.zeta.config.UploadProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CognitionVideoStorageTest {

    private static final byte[] MP4_HEADER = new byte[] {
            0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'
    };

    @TempDir
    Path tempDir;

    @Test
    void storesVideoUnderJarRelativeResourcePath() {
        CognitionVideoStorage storage = storage();
        String path = storage.store(file("lesson.mp4", "video/mp4", 1024));

        assertTrue(path.matches("resource/video/[0-9a-f-]{36}\\.mp4"));
        assertTrue(storage.exists(path));
        assertTrue(Files.isRegularFile(tempDir.resolve(path)));
    }

    @Test
    void acceptsExactlyFiftyMibAndRejectsLargerFile() {
        CognitionVideoStorage storage = storage();
        String path = storage.store(file("limit.mp4", "video/mp4", CognitionVideoStorage.MAX_VIDEO_BYTES));
        assertTrue(storage.exists(path));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> storage.store(file("too-large.mp4", "video/mp4", CognitionVideoStorage.MAX_VIDEO_BYTES + 1)));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, ex.getStatus());
    }

    @Test
    void rejectsWrongMimeExtensionSignatureAndManagedPathTraversal() {
        CognitionVideoStorage storage = storage();

        assertThrows(ResponseStatusException.class, () -> storage.store(file("lesson.mov", "video/mp4", 12)));
        assertThrows(ResponseStatusException.class, () -> storage.store(file("lesson.mp4", "application/octet-stream", 12)));
        assertThrows(ResponseStatusException.class,
                () -> storage.store(new SizedMultipartFile("lesson.mp4", "video/mp4", 12, new byte[12])));
        assertThrows(ResponseStatusException.class,
                () -> storage.normalizeManagedPath("resource/video/../../secret.mp4"));
    }

    @Test
    void deletesManagedVideo() {
        CognitionVideoStorage storage = storage();
        String path = storage.store(file("lesson.mp4", "video/mp4", 1024));
        storage.delete(path);
        assertFalse(storage.exists(path));
    }

    private CognitionVideoStorage storage() {
        UploadProperties properties = new UploadProperties();
        properties.setVideoStorageDir(tempDir.toString());
        return new CognitionVideoStorage(properties);
    }

    private MultipartFile file(String name, String contentType, long size) {
        return new SizedMultipartFile(name, contentType, size, MP4_HEADER);
    }

    private static class SizedMultipartFile implements MultipartFile {
        private final String name;
        private final String contentType;
        private final long size;
        private final byte[] data;

        SizedMultipartFile(String name, String contentType, long size, byte[] data) {
            this.name = name;
            this.contentType = contentType;
            this.size = size;
            this.data = data;
        }

        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return size == 0; }
        @Override public long getSize() { return size; }
        @Override public byte[] getBytes() { return data; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(data); }
        @Override public void transferTo(File dest) throws IOException { Files.write(dest.toPath(), data); }
    }
}
