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
    @Column(name = "confirmation_token", unique = true, length = 64) private String confirmationToken;
    @Column(name = "confirmed_at") private Instant confirmedAt;
    @Column(name = "unsubscribed_at") private Instant unsubscribedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
