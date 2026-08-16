package com.signalnotes.blog.repository;

import com.signalnotes.blog.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PostRepository extends JpaRepository<Post, Long> {
    Optional<Post> findBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
    long countByStatus(PostStatus status);
    long countByCover(String cover);
    List<Post> findByStatusAndScheduledAtBefore(PostStatus status, java.time.Instant now);
    List<Post> findTop5ByStatusOrderByViewsDesc(PostStatus status);
    @Query("""
        select distinct p from Post p left join p.tags t
        where p.status = :status
        and (:query is null or lower(p.title) like lower(concat('%', :query, '%')) or lower(p.excerpt) like lower(concat('%', :query, '%')) or lower(cast(p.content as string)) like lower(concat('%', :query, '%')))
        and (:category is null or p.category.name = :category)
        and (:tag is null or t = :tag)
        order by p.publishedAt desc, p.id desc
        """)
    List<Post> searchPublished(@Param("status") PostStatus status, @Param("query") String query, @Param("category") String category, @Param("tag") String tag);
}
