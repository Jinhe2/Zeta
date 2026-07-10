package com.zeta.business.media;

import com.zeta.business.auth.AuthService;
import com.zeta.business.devicedisplay.DeviceDisplayItem;
import com.zeta.business.devicedisplay.DeviceDisplayItemRepository;
import com.zeta.business.logicnodecognition.LogicNodeCognitionItemRepository;
import com.zeta.business.learningresource.LearningResourceRepository;
import com.zeta.config.UploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CognitionVideoControllerTest {

    @TempDir
    Path tempDir;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        UploadProperties properties = new UploadProperties();
        properties.setVideoStorageDir(tempDir.toString());
        CognitionVideoStorage storage = new CognitionVideoStorage(properties);
        String path = "resource/video/123e4567-e89b-12d3-a456-426614174000.mp4";
        Files.createDirectories(tempDir.resolve("resource/video"));
        Files.write(tempDir.resolve(path), "0123456789abcdef".getBytes("UTF-8"));

        DeviceDisplayItem item = new DeviceDisplayItem();
        item.setVideoPath(path);
        DeviceDisplayItemRepository deviceRepository = mock(DeviceDisplayItemRepository.class);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(item));

        CognitionVideoController controller = new CognitionVideoController(
                storage,
                deviceRepository,
                mock(LogicNodeCognitionItemRepository.class),
                mock(LearningResourceRepository.class),
                mock(AuthService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void servesFullVideoWithRangeCapability() throws Exception {
        mockMvc.perform(get("/api/videos/device-display/1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Accept-Ranges", "bytes"))
                .andExpect(content().contentType("video/mp4"))
                .andExpect(content().bytes("0123456789abcdef".getBytes("UTF-8")));
    }

    @Test
    void servesPartialContentForRangeRequest() throws Exception {
        mockMvc.perform(get("/api/videos/device-display/1").header("Range", "bytes=2-5"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 2-5/16"))
                .andExpect(content().bytes("2345".getBytes("UTF-8")));
    }
}
