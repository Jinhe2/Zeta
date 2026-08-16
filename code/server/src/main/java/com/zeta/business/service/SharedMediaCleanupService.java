package com.zeta.business.service;

import com.zeta.business.entities.devicedisplay.DeviceDisplayItemRepository;
import com.zeta.business.entities.cabinetdisplay.CabinetDisplayItemRepository;
import com.zeta.business.entities.drawinglearning.DrawingPageRepository;
import com.zeta.business.entities.learningresource.LearningResourceRepository;
import com.zeta.business.entities.logicnodecognition.LogicNodeCognitionItemRepository;
import com.zeta.business.entities.samplingtest.SamplingTestItemRepository;
import com.zeta.business.storage.CognitionVideoStorage;
import com.zeta.business.storage.CabinetDisplayImageStorage;
import com.zeta.business.storage.DeviceDisplayImageStorage;
import com.zeta.business.storage.LearningResourceStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/** Ensures a shared media file is only removed after its final database reference disappears. */
@Service
public class SharedMediaCleanupService {
  private final DeviceDisplayItemRepository deviceItems;
  private final CabinetDisplayItemRepository cabinetItems;
  private final DrawingPageRepository drawingPages;
  private final LogicNodeCognitionItemRepository logicItems;
  private final SamplingTestItemRepository samplingItems;
  private final LearningResourceRepository resources;
  private final CognitionVideoStorage cognitionVideoStorage;
  private final LearningResourceStorage learningResourceStorage;
  private final CabinetDisplayImageStorage cabinetImageStorage;
  private final DeviceDisplayImageStorage deviceImageStorage;

  public SharedMediaCleanupService(
      DeviceDisplayItemRepository deviceItems,
      CabinetDisplayItemRepository cabinetItems,
      DrawingPageRepository drawingPages,
      LogicNodeCognitionItemRepository logicItems,
      SamplingTestItemRepository samplingItems,
      LearningResourceRepository resources,
      CognitionVideoStorage cognitionVideoStorage,
      LearningResourceStorage learningResourceStorage,
      CabinetDisplayImageStorage cabinetImageStorage,
      DeviceDisplayImageStorage deviceImageStorage) {
    this.deviceItems = deviceItems;
    this.cabinetItems = cabinetItems;
    this.drawingPages = drawingPages;
    this.logicItems = logicItems;
    this.samplingItems = samplingItems;
    this.resources = resources;
    this.cognitionVideoStorage = cognitionVideoStorage;
    this.learningResourceStorage = learningResourceStorage;
    this.cabinetImageStorage = cabinetImageStorage;
    this.deviceImageStorage = deviceImageStorage;
  }

  public void scheduleCognitionVideoDeletion(String path) {
    schedule(path, () -> {
      if (!deviceItems.existsByVideoPath(path)
          && !logicItems.existsByVideoPath(path)
          && !samplingItems.existsByVideoPath(path)) {
        cognitionVideoStorage.delete(path);
      }
    });
  }

  public void scheduleLearningResourceDeletion(String path) {
    schedule(path, () -> {
      if (!resources.existsByFilePath(path)) learningResourceStorage.delete(path);
    });
  }

  public void scheduleCabinetImageDeletion(String path) {
    schedule(path, () -> {
      if (!cabinetItems.existsByImageUrl(path) && !drawingPages.existsByImageUrl(path)) {
        cabinetImageStorage.deleteIfManaged(path);
      }
    });
  }

  public void scheduleDeviceImageDeletion(String path) {
    schedule(path, () -> {
      if (!deviceItems.existsByImageUrl(path) && !logicItems.existsByImageUrl(path)) {
        deviceImageStorage.deleteIfManaged(path);
      }
    });
  }

  private void schedule(String path, Runnable cleanup) {
    if (!StringUtils.hasText(path)) return;
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      cleanup.run();
      return;
    }
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCommit() {
        try { cleanup.run(); } catch (RuntimeException ignored) { /* committed DB state wins */ }
      }
    });
  }
}
