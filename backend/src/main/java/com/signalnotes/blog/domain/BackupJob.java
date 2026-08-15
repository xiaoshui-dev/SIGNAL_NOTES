package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "backup_jobs", indexes = @Index(name = "idx_backup_created", columnList = "created_at"))
@Getter @Setter @NoArgsConstructor
public class BackupJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 255) private String filename;
    @Column(nullable = false, length = 20) private String status = "RUNNING";
    @Column(nullable = false) private Long size = 0L;
    @Column(length = 64) private String checksum;
    @Column(nullable = false) private boolean verified = false;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "verified_at") private Instant verifiedAt;
}
