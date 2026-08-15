package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "post_revisions", uniqueConstraints = @UniqueConstraint(name = "uk_revision_post_version", columnNames = {"post_id", "version_no"}))
@Getter @Setter @NoArgsConstructor
public class PostRevision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "post_id", nullable = false) private Post post;
    @Column(name = "version_no", nullable = false) private Integer versionNo;
    @Column(nullable = false, length = 240) private String title;
    @Column(nullable = false, length = 1000) private String excerpt = "";
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String content = "";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PostStatus status;
    @Column(nullable = false, length = 80) private String editor = "system";
    @Column(name = "change_summary", length = 500) private String changeSummary = "保存文章";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
