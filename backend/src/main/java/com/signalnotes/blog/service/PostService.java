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
    private final PostRevisionRepository revisions;
    public PostService(PostRepository posts, CategoryRepository categories, PostRevisionRepository revisions) { this.posts = posts; this.categories = categories; this.revisions = revisions; }

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
        post.setSlug(input.slug()); post.setTitle(input.title()); post.setExcerpt(input.excerpt() == null ? "" : input.excerpt()); post.setContent(input.content() == null ? "" : input.content()); post.setCover(input.cover()); post.setCoverAlt(input.coverAlt()); post.setCategory(category);
        post.setTags(new LinkedHashSet<>(input.tags() == null ? List.of() : input.tags())); post.setStatus(input.status()); post.setPublishedAt(input.publishedAt()); post.setScheduledAt(input.scheduledAt()); post.setSeoTitle(input.seoTitle()); post.setSeoDescription(input.seoDescription()); post.setCanonicalUrl(input.canonicalUrl()); post.setPinned(input.pinned()); post.setDeletedAt(input.status() == PostStatus.TRASHED ? Optional.ofNullable(post.getDeletedAt()).orElse(java.time.Instant.now()) : null); post.setUpdatedAt(LocalDate.now()); post.setReadMinutes(input.readMinutes() == null ? 5 : input.readMinutes()); post.setAuthorName(input.authorName() == null ? "林默" : input.authorName());
        Post saved = posts.save(post);
        PostRevision revision = new PostRevision(); revision.setPost(saved); revision.setVersionNo(revisions.findTopByPostIdOrderByVersionNoDesc(saved.getId()).map(item -> item.getVersionNo() + 1).orElse(1)); revision.setTitle(saved.getTitle()); revision.setExcerpt(saved.getExcerpt()); revision.setContent(saved.getContent()); revision.setStatus(saved.getStatus()); revision.setEditor(saved.getAuthorName()); revisions.save(revision);
        return PostView.from(saved);
    }

    @Transactional public void delete(Long id) { Post post = posts.findById(id).orElseThrow(() -> new EntityNotFoundException("文章不存在")); post.setStatus(PostStatus.TRASHED); post.setDeletedAt(java.time.Instant.now()); post.setUpdatedAt(LocalDate.now()); posts.save(post); }

    @Transactional(readOnly = true)
    public List<PostRevision> revisions(Long id) { if (!posts.existsById(id)) throw new EntityNotFoundException("文章不存在"); return revisions.findByPostIdOrderByVersionNoDesc(id); }

    @Transactional
    public PostView restoreRevision(Long id, Long revisionId) {
        Post post = posts.findById(id).orElseThrow(() -> new EntityNotFoundException("文章不存在"));
        PostRevision revision = revisions.findById(revisionId).filter(item -> item.getPost().getId().equals(id)).orElseThrow(() -> new EntityNotFoundException("版本不存在"));
        post.setTitle(revision.getTitle()); post.setExcerpt(revision.getExcerpt()); post.setContent(revision.getContent()); post.setStatus(PostStatus.DRAFT); post.setUpdatedAt(LocalDate.now());
        Post saved = posts.save(post); PostRevision restored = new PostRevision(); restored.setPost(saved); restored.setVersionNo(revisions.findTopByPostIdOrderByVersionNoDesc(id).map(item -> item.getVersionNo() + 1).orElse(1)); restored.setTitle(saved.getTitle()); restored.setExcerpt(saved.getExcerpt()); restored.setContent(saved.getContent()); restored.setStatus(saved.getStatus()); restored.setEditor("restore"); restored.setChangeSummary("从版本 #" + revision.getVersionNo() + " 恢复"); revisions.save(restored); return PostView.from(saved);
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String slugify(String value) { return "category-" + Integer.toUnsignedString(value.hashCode(), 36); }

    public record PostInput(@NotBlank String slug, @NotBlank String title, String excerpt, String content, String cover, String coverAlt, @NotBlank String category, Set<String> tags, @NotNull PostStatus status, String authorName, LocalDate publishedAt, @Min(1) @Max(120) Integer readMinutes, java.time.Instant scheduledAt, String seoTitle, String seoDescription, String canonicalUrl, boolean pinned) {}
    public record PostView(Long id, String slug, String title, String excerpt, String content, String cover, String coverAlt, String category, Set<String> tags, PostStatus status, String authorName, LocalDate publishedAt, LocalDate updatedAt, Integer readMinutes, Long views, java.time.Instant scheduledAt, java.time.Instant deletedAt, String seoTitle, String seoDescription, String canonicalUrl, boolean pinned) {
        static PostView from(Post p) { return new PostView(p.getId(), p.getSlug(), p.getTitle(), p.getExcerpt(), p.getContent(), p.getCover(), p.getCoverAlt(), p.getCategory() == null ? "未分类" : p.getCategory().getName(), p.getTags(), p.getStatus(), p.getAuthorName(), p.getPublishedAt(), p.getUpdatedAt(), p.getReadMinutes(), p.getViews(), p.getScheduledAt(), p.getDeletedAt(), p.getSeoTitle(), p.getSeoDescription(), p.getCanonicalUrl(), p.isPinned()); }
    }
}
