package com.signalnotes.blog.repository;

import com.signalnotes.blog.domain.BackupJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BackupJobRepository extends JpaRepository<BackupJob, Long> {
    List<BackupJob> findTop20ByOrderByCreatedAtDesc();
}
