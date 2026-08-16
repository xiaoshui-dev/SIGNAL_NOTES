package com.signalnotes.blog.service;
import com.signalnotes.blog.domain.AuditLog;
import com.signalnotes.blog.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository logs;
    public AuditService(AuditLogRepository logs) { this.logs = logs; }
    public void log(String actor, String action, String target) {
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setTarget(target == null ? "" : target.length() <= 300 ? target : target.substring(0, 297) + "...");
        logs.save(log);
    }
}
