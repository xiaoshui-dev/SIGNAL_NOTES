package com.signalnotes.blog.service;

import com.signalnotes.blog.domain.PostStatus;
import com.signalnotes.blog.repository.PostRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service
public class ScheduledPublishingService {
    private final PostRepository posts;
    public ScheduledPublishingService(PostRepository posts) { this.posts = posts; }
    @Scheduled(fixedDelayString = "${app.publish-check-ms:60000}") @Transactional
    public void publishDuePosts() { posts.findByStatusAndScheduledAtBefore(PostStatus.SCHEDULED, Instant.now()).forEach(post -> { post.setStatus(PostStatus.PUBLISHED); post.setPublishedAt(LocalDate.now()); post.setUpdatedAt(LocalDate.now()); }); }
}
