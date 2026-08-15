package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.*;
import java.util.*;

@Entity @Table(name = "posts", indexes = {@Index(name = "idx_posts_status_published", columnList = "status,published_at"), @Index(name = "idx_posts_category", columnList = "category_id")})
@Getter @Setter @NoArgsConstructor
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @NotBlank @Column(nullable = false, unique = true, length = 180) private String slug;
    @NotBlank @Column(nullable = false, length = 240) private String title;
    @Column(nullable = false, length = 1000) private String excerpt = "";
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT") private String content = "";
    @Column(length = 500) private String cover;
    @Column(name = "cover_alt", length = 300) private String coverAlt;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "category_id") private Category category;
    @ElementCollection(fetch = FetchType.EAGER) @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id")) @Column(name = "tag", length = 80) private Set<String> tags = new LinkedHashSet<>();
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PostStatus status = PostStatus.DRAFT;
    @Column(name = "author_name", nullable = false, length = 80) private String authorName = "林默";
    @Column(name = "published_at") private LocalDate publishedAt;
    @Column(name = "updated_at", nullable = false) private LocalDate updatedAt = LocalDate.now();
    @Column(name = "read_minutes", nullable = false) private Integer readMinutes = 5;
    @Column(nullable = false) private Long views = 0L;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
