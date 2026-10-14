package com.zeta.business.entities.presence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentActivitySessionRepository extends JpaRepository<StudentActivitySession, Long> {
  List<StudentActivitySession> findByUserIdAndStatus(Long userId, StudentActivityStatus status);

  List<StudentActivitySession> findByDeviceBindIdAndStatus(
      String deviceBindId, StudentActivityStatus status);

  List<StudentActivitySession> findByStatusAndExpiresAtAfter(
      StudentActivityStatus status, Instant expiresAt);

  List<StudentActivitySession> findByStatusAndExpiresAtBefore(
      StudentActivityStatus status, Instant expiresAt);

  @Query("select s from StudentActivitySession s where s.startedAt < :to and s.expiresAt > :from "
      + "and (:userId is null or s.userId = :userId) "
      + "and (:cabinetId is null or s.cabinetId = :cabinetId) order by s.startedAt desc")
  List<StudentActivitySession> findOverlapping(
      @Param("from") Instant from, @Param("to") Instant to,
      @Param("userId") Long userId, @Param("cabinetId") Long cabinetId);

  @Query("select s from StudentActivitySession s where s.cabinetId = :cabinetId "
      + "and s.startedAt < :to and s.expiresAt > :from "
      + "and (:userId is null or s.userId = :userId)")
  Page<StudentActivitySession> findCabinetHistory(
      @Param("cabinetId") Long cabinetId, @Param("from") Instant from,
      @Param("to") Instant to, @Param("userId") Long userId, Pageable pageable);
}
