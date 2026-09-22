package com.zeta.business.entities.wholeexperiment;

import java.util.*;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WholeExperimentRepository extends JpaRepository<WholeExperiment, Long> {
  Optional<WholeExperiment> findByIdAndUserId(Long id, Long userId);
  Optional<WholeExperiment> findByUserIdAndDeviceIdAndMemberSignature(
      Long userId, Long deviceId, String memberSignature);
  @Query("select e from WholeExperiment e where e.userId=:userId and e.deviceId=:deviceId "
      + "order by case when e.lastStartedAt is null then 1 else 0 end, "
      + "e.lastStartedAt desc, e.id desc")
  List<WholeExperiment> findByUserIdAndDeviceIdOrderByRecent(
      @Param("userId") Long userId, @Param("deviceId") Long deviceId);

  @Modifying
  @Query(value = "INSERT INTO whole_experiment (user_id, device_id, member_signature, created_at) "
      + "VALUES (:userId, :deviceId, :signature, CURRENT_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE id=id",
      nativeQuery = true)
  void insertIfAbsent(@Param("userId") Long userId, @Param("deviceId") Long deviceId,
      @Param("signature") String signature);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from WholeExperiment e where e.userId=:userId and e.deviceId=:deviceId "
      + "and e.memberSignature=:signature")
  WholeExperiment lockBySignature(@Param("userId") Long userId, @Param("deviceId") Long deviceId,
      @Param("signature") String signature);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from WholeExperiment e where e.id=:id")
  Optional<WholeExperiment> lockById(@Param("id") Long id);
}
