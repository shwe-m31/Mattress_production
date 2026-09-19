package com.peps.production.repository;

import com.peps.production.model.DowntimeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DowntimeEventRepository extends JpaRepository<DowntimeEvent, Long> {
    List<DowntimeEvent> findByStartTimeBetweenOrderByStartTime(LocalDateTime start, LocalDateTime end);
    List<DowntimeEvent> findByShiftAndStatus(String shift, String status);
    
    @Query("SELECT COALESCE(SUM(d.durationMinutes), 0) FROM DowntimeEvent d WHERE d.startTime BETWEEN :start AND :end")
    long sumDurationMinutesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT COALESCE(SUM(d.durationMinutes), 0) FROM DowntimeEvent d WHERE d.shift = :shift AND d.startTime BETWEEN :start AND :end")
    long sumDurationMinutesByShiftBetween(@Param("shift") String shift, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}