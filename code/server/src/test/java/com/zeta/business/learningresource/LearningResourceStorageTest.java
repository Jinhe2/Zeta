package com.zeta.business.learningresource;

import com.zeta.config.UploadProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningResourceStorageTest {
    @TempDir
    Path tempDir;

    @Test
    void storesPdfAndVideoInTheirExpectedDirectories() {
        LearningResourceStorage storage = storage();
        LearningResourceStorage.StoredFile pdf = storage.store(LearningResourceType.MANUAL,
                new MockMultipartFile("file", "manual.pdf", "application/pdf", "%PDF-1.7".getBytes()));
        LearningResourceStorage.StoredFile video = storage.store(LearningResourceType.VIDEO_COURSE,
                new MockMultipartFile("file", "lesson.mp4", "video/mp4",
                        new byte[] { 0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm' }));

        assertTrue(pdf.getPath().matches("resource/pdf/[0-9a-f-]{36}\\.pdf"));
        assertTrue(video.getPath().matches("resource/video/[0-9a-f-]{36}\\.mp4"));
        assertTrue(storage.load(pdf.getPath()).exists());
        assertTrue(storage.load(video.getPath()).exists());
    }

    @Test
    void rejectsMismatchedFileKindsAndInvalidHeaders() {
        LearningResourceStorage storage = storage();
        ResponseStatusException wrongPdf = assertThrows(ResponseStatusException.class, () -> storage.store(
                LearningResourceType.DRAWING,
                new MockMultipartFile("file", "drawing.pdf", "application/pdf", "not a pdf".getBytes())));
        ResponseStatusException wrongVideo = assertThrows(ResponseStatusException.class, () -> storage.store(
                LearningResourceType.VIDEO_COURSE,
                new MockMultipartFile("file", "lesson.mov", "video/mp4", new byte[12])));

        assertEquals(HttpStatus.BAD_REQUEST, wrongPdf.getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, wrongVideo.getStatus());
    }

    private LearningResourceStorage storage() {
        UploadProperties properties = new UploadProperties();
        properties.setResourceStorageDir(tempDir.toString());
        return new LearningResourceStorage(properties);
    }
}
