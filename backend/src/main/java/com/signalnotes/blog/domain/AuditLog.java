package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "audit_logs") @Getter @Setter @NoArgsConstructor
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 80) private String actor;
    @Column(nullable = false, length = 100) private String action;
    @Column(nullable = false, length = 300) private String target;
    @Column(nullable = false, length = 20) private String result = "SUCCESS";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
