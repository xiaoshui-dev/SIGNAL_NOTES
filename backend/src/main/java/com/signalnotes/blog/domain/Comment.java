package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "comments", indexes = @Index(name = "idx_comments_post_status", columnList = "post_slug,status")) @Getter @Setter @NoArgsConstructor
public class Comment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(name = "post_slug", nullable = false, length = 180) private String postSlug;
    @NotBlank @Size(max = 80) @Column(name = "author_name", nullable = false, length = 80) private String authorName;
    @Email @Column(length = 180) private String email;
    @NotBlank @Size(min = 2, max = 2000) @Column(nullable = false, length = 2000) private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CommentStatus status = CommentStatus.PENDING;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
