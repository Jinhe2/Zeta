package com.zeta.business.service;

import static org.mockito.Mockito.*;

import com.zeta.business.entities.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.business.entities.devicedisplay.DeviceDisplayItemRepository;
import com.zeta.business.entities.drawinglearning.DrawingPageRepository;
import com.zeta.business.entities.learningresource.LearningResourceRepository;
import com.zeta.business.entities.logicnodecognition.LogicNodeCognitionItemRepository;
import com.zeta.business.entities.samplingtest.SamplingTestItemRepository;
import com.zeta.business.storage.*;
import org.junit.jupiter.api.Test;

class SharedMediaCleanupServiceTest {
  @Test
  void keepsVideoWhileAnotherRecordReferencesIt() {
    Fixture fixture = new Fixture();
    when(fixture.deviceItems.existsByVideoPath("resource/video/a.mp4")).thenReturn(true);
    fixture.service.scheduleCognitionVideoDeletion("resource/video/a.mp4");
    verify(fixture.videoStorage, never()).delete(anyString());
  }

  @Test
  void deletesVideoAfterFinalReferenceDisappears() {
    Fixture fixture = new Fixture();
    fixture.service.scheduleCognitionVideoDeletion("resource/video/a.mp4");
    verify(fixture.videoStorage).delete("resource/video/a.mp4");
  }

  private static class Fixture {
    final DeviceDisplayItemRepository deviceItems = mock(DeviceDisplayItemRepository.class);
    final LogicNodeCognitionItemRepository logicItems = mock(LogicNodeCognitionItemRepository.class);
    final SamplingTestItemRepository samplingItems = mock(SamplingTestItemRepository.class);
    final LearningResourceRepository resources = mock(LearningResourceRepository.class);
    final CognitionVideoStorage videoStorage = mock(CognitionVideoStorage.class);
    final SharedMediaCleanupService service = new SharedMediaCleanupService(
        deviceItems, mock(CabinetDisplayItemRepository.class), mock(DrawingPageRepository.class),
        logicItems, samplingItems, resources, videoStorage, mock(LearningResourceStorage.class),
        mock(CabinetDisplayImageStorage.class), mock(DeviceDisplayImageStorage.class));
  }
}
