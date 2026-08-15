package com.signalnotes.blog.repository;
import com.signalnotes.blog.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> { List<AuditLog> findTop50ByOrderByCreatedAtDesc(); }
