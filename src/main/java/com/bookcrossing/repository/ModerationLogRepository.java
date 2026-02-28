package com.bookcrossing.repository;

import com.bookcrossing.model.ModerationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {
    List<ModerationLog> findAllByOrderByCreatedAtDesc();
    List<ModerationLog> findByModeratorUsernameOrderByCreatedAtDesc(String username);
}