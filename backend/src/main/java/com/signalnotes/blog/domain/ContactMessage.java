package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "contact_messages", indexes = @Index(name = "idx_contact_created", columnList = "created_at"))
@Getter @Setter @NoArgsConstructor
public class ContactMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Size(max = 80) @Column(nullable = false, length = 80) private String name;
    @NotBlank @Email @Size(max = 180) @Column(nullable = false, length = 180) private String email;
    @NotBlank @Size(max = 180) @Column(nullable = false, length = 180) private String subject;
    @NotBlank @Size(max = 2000) @Column(nullable = false, length = 2000) private String message;
    @Column(nullable = false, length = 30) private String status = "RECEIVED";
    @Column(nullable = false, unique = true, length = 40) private String ticket;
    @Column(nullable = false) private boolean consent;
    @JsonIgnore @Column(name = "idempotency_key", unique = true, length = 100) private String idempotencyKey;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
