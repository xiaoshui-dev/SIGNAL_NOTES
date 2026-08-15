package com.signalnotes.blog.controller;

import com.signalnotes.blog.domain.*;
import com.signalnotes.blog.repository.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/comments")
public class CommentController {
    private final CommentRepository comments;
    private final PostRepository posts;
    public CommentController(CommentRepository comments, PostRepository posts) { this.comments = comments; this.posts = posts; }
    @GetMapping public List<Comment> list(@RequestParam String postSlug) { return comments.findByPostSlugAndStatusOrderByCreatedAtDesc(postSlug, CommentStatus.APPROVED); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Comment create(@Valid @RequestBody Comment input) {
        if (posts.findBySlug(input.getPostSlug()).filter(post -> post.getStatus() == PostStatus.PUBLISHED).isEmpty()) throw new IllegalArgumentException("文章不存在或未发布");
        if (input.getParentId() != null) comments.findById(input.getParentId()).filter(parent -> parent.getPostSlug().equals(input.getPostSlug()) && parent.getStatus() == CommentStatus.APPROVED).orElseThrow(() -> new IllegalArgumentException("回复的评论不存在或不可回复"));
        input.setId(null); input.setReportCount(0); input.setReportReason(null); input.setStatus(looksLikeSpam(input.getContent()) ? CommentStatus.SPAM : CommentStatus.PENDING); return comments.save(input);
    }
    @PostMapping("/{id}/report") public Map<String,Object> report(@PathVariable Long id,@RequestBody Map<String,String> body) { Comment value=comments.findById(id).filter(item->item.getStatus()==CommentStatus.APPROVED).orElseThrow(()->new IllegalArgumentException("评论不存在或不可举报"));value.setReportCount(value.getReportCount()+1);value.setReportReason(body.getOrDefault("reason","其他").substring(0,Math.min(240,body.getOrDefault("reason","其他").length())));comments.save(value);return Map.of("status","RECEIVED","message","举报已提交，管理员会进行复核"); }
    private boolean looksLikeSpam(String content){String lower=content.toLowerCase();return lower.matches(".*(https?://.*){3,}.*")||lower.matches(".*(.)\\1{11,}.*");}
}
