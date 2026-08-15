package com.signalnotes.blog.controller;

import com.signalnotes.blog.domain.*;
import com.signalnotes.blog.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/comments")
public class CommentController {
    private final CommentRepository comments;
    private final PostRepository posts;
    public CommentController(CommentRepository comments, PostRepository posts) { this.comments = comments; this.posts = posts; }
    @GetMapping public List<Comment> list(@RequestParam String postSlug) { return comments.findByPostSlugAndStatusOrderByCreatedAtDesc(postSlug, CommentStatus.APPROVED); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Comment create(@Valid @RequestBody Comment input) {
        if (posts.findBySlug(input.getPostSlug()).filter(post -> post.getStatus() == PostStatus.PUBLISHED).isEmpty()) throw new IllegalArgumentException("文章不存在或未发布");
        input.setId(null); input.setStatus(CommentStatus.PENDING); return comments.save(input);
    }
}
