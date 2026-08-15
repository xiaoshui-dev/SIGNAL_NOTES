package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "subscriptions") @Getter @Setter @NoArgsConstructor
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Email @Column(nullable = false, unique = true, length = 180) private String email;
    @Column(nullable = false, length = 20) private String status = "PENDING";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
