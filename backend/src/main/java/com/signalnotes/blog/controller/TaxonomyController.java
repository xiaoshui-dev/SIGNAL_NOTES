package com.signalnotes.blog.controller;

import com.signalnotes.blog.domain.Category;
import com.signalnotes.blog.domain.Tag;
import com.signalnotes.blog.service.PostService;
import com.signalnotes.blog.repository.CategoryRepository;
import com.signalnotes.blog.repository.TagRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class TaxonomyController {
    private final CategoryRepository categories;
    private final TagRepository tags;
    private final PostService posts;

    public TaxonomyController(CategoryRepository categories, TagRepository tags, PostService posts) {
        this.categories = categories;
        this.tags = tags;
        this.posts = posts;
    }

    @GetMapping("/api/categories")
    public List<Map<String, Object>> categories() {
        return categories.findAll().stream()
            .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
            .map(category -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", category.getId()); result.put("name", category.getName()); result.put("slug", category.getSlug());
                result.put("description", Optional.ofNullable(category.getDescription()).orElse(""));
                result.put("count", posts.published(null, category.getName(), null).size());
                return result;
            })
            .toList();
    }

    @GetMapping("/api/tags")
    public List<Map<String, Object>> tags() {
        return tags.findAll().stream()
            .sorted(Comparator.comparing(Tag::getName, String.CASE_INSENSITIVE_ORDER))
            .map(tag -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("id", tag.getId()); result.put("name", tag.getName()); result.put("slug", tag.getSlug());
                result.put("description", Optional.ofNullable(tag.getDescription()).orElse(""));
                result.put("count", posts.published(null, null, tag.getName()).size());
                return result;
            })
            .toList();
    }
}
