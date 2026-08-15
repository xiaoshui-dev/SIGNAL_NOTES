package com.signalnotes.blog.service;

import com.signalnotes.blog.domain.*;
import com.signalnotes.blog.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
public class PostService {
    private final PostRepository posts;
    private final CategoryRepository categories;
    public PostService(PostRepository posts, CategoryRepository categories) { this.posts = posts; this.categories = categories; }

    @Transactional(readOnly = true)
    public List<PostView> published(String query, String category, String tag) {
        return posts.searchPublished(PostStatus.PUBLISHED, blankToNull(query), blankToNull(category), blankToNull(tag)).stream().map(PostView::from).toList();
    }

    @Transactional
    public PostView publishedBySlug(String slug) {
        Post post = posts.findBySlug(slug).filter(value -> value.getStatus() == PostStatus.PUBLISHED).orElseThrow(() -> new EntityNotFoundException("文章不存在或未发布"));
        post.setViews(post.getViews() + 1);
        return PostView.from(post);
    }

    @Transactional(readOnly = true)
    public List<PostView> all() { return posts.findAll().stream().sorted(Comparator.comparing(Post::getUpdatedAt).reversed()).map(PostView::from).toList(); }

    @Transactional
    public PostView save(Long id, PostInput input) {
        Post post = id == null ? new Post() : posts.findById(id).orElseThrow(() -> new EntityNotFoundException("文章不存在"));
        if (posts.existsBySlugAndIdNot(input.slug(), id == null ? -1L : id)) throw new IllegalArgumentException("slug 已存在");
        Category category = categories.findByName(input.category()).orElseGet(() -> { Category value = new Category(); value.setName(input.category()); value.setSlug(slugify(input.category())); value.setDescription(""); return categories.save(value); });
        post.setSlug(input.slug()); post.setTitle(input.title()); post.setExcerpt(input.excerpt()); post.setContent(input.content()); post.setCover(input.cover()); post.setCoverAlt(input.coverAlt()); post.setCategory(category);
        post.setTags(new LinkedHashSet<>(input.tags() == null ? List.of() : input.tags())); post.setStatus(input.status()); post.setPublishedAt(input.publishedAt()); post.setUpdatedAt(LocalDate.now()); post.setReadMinutes(input.readMinutes()); post.setAuthorName(input.authorName() == null ? "林默" : input.authorName());
        return PostView.from(posts.save(post));
    }

    @Transactional public void delete(Long id) { if (!posts.existsById(id)) throw new EntityNotFoundException("文章不存在"); posts.deleteById(id); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String slugify(String value) { return "category-" + Integer.toUnsignedString(value.hashCode(), 36); }

    public record PostInput(@NotBlank String slug, @NotBlank String title, String excerpt, String content, String cover, String coverAlt, @NotBlank String category, Set<String> tags, @NotNull PostStatus status, String authorName, LocalDate publishedAt, @Min(1) @Max(120) Integer readMinutes) {}
    public record PostView(Long id, String slug, String title, String excerpt, String content, String cover, String coverAlt, String category, Set<String> tags, PostStatus status, String authorName, LocalDate publishedAt, LocalDate updatedAt, Integer readMinutes, Long views) {
        static PostView from(Post p) { return new PostView(p.getId(), p.getSlug(), p.getTitle(), p.getExcerpt(), p.getContent(), p.getCover(), p.getCoverAlt(), p.getCategory() == null ? "未分类" : p.getCategory().getName(), p.getTags(), p.getStatus(), p.getAuthorName(), p.getPublishedAt(), p.getUpdatedAt(), p.getReadMinutes(), p.getViews()); }
    }
}
