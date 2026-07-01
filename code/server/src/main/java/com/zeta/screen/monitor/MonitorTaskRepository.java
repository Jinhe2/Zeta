package com.zeta.screen.monitor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitorTaskRepository extends JpaRepository<MonitorTask, Long> {

    Optional<MonitorTask> findByUuid(String uuid);

    List<MonitorTask> findByLogicDiagramIdOrderByCreatedAtDesc(Long logicDiagramId);

    List<MonitorTask> findByIedDeviceIdOrderByCreatedAtDesc(Long iedDeviceId);

    List<MonitorTask> findByStateOrderByCreatedAtDesc(MonitorTask.TaskState state);
}
