package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "media_assets") @Getter @Setter @NoArgsConstructor
public class MediaAsset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 255) private String filename;
    @Column(nullable = false, unique = true, length = 500) private String url;
    @Column(name = "mime_type", nullable = false, length = 100) private String mimeType;
    @Column(nullable = false) private Long size;
    @Column(name = "alt_text", length = 300) private String altText;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
