package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "site_users") @Getter @Setter @NoArgsConstructor
public class SiteUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false, unique = true, length = 180) private String email;
    @Column(nullable = false, length = 30) private String role = "AUTHOR";
    @Column(nullable = false, length = 20) private String status = "ACTIVE";
    @Column(name = "last_login_at") private Instant lastLoginAt;
}
