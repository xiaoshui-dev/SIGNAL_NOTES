package com.signalnotes.blog.controller;

import com.signalnotes.blog.service.PostService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/posts")
public class PostController {
    private final PostService service;
    public PostController(PostService service) { this.service = service; }
    @GetMapping public List<PostService.PostView> list(@RequestParam(required = false) String q, @RequestParam(required = false) String category, @RequestParam(required = false) String tag) { return service.published(q, category, tag); }
    @GetMapping("/{slug}") public PostService.PostView get(@PathVariable String slug) { return service.publishedBySlug(slug); }
}
