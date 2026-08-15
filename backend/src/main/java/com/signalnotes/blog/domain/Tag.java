package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "tags") @Getter @Setter @NoArgsConstructor
public class Tag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, unique = true, length = 80) private String name;
    @NotBlank @Column(nullable = false, unique = true, length = 100) private String slug;
    @Column(length = 500) private String description;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
