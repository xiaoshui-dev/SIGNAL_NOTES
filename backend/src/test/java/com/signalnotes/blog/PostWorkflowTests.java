package com.signalnotes.blog;

import com.signalnotes.blog.domain.Post;
import com.signalnotes.blog.domain.PostStatus;
import com.signalnotes.blog.repository.PostRepository;
import com.signalnotes.blog.repository.PostRevisionRepository;
import com.signalnotes.blog.repository.SettingRepository;
import com.signalnotes.blog.repository.UserRepository;
import com.signalnotes.blog.service.PostService;
import com.signalnotes.blog.domain.SiteSetting;
import com.signalnotes.blog.service.AdminUserBootstrap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostWorkflowTests {
    @Autowired PostService service;
    @Autowired PostRepository posts;
    @Autowired PostRevisionRepository revisions;
    @Autowired SettingRepository settings;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        revisions.deleteAll();
        posts.deleteAll();
    }

    @Test
    void savingAnArticleCreatesAnAuditableRevision() {
        var saved = service.save(null, new PostService.PostInput(
            "revision-test", "可追溯文章", "摘要", "正文", null, "", "系统设计",
            Set.of("工程"), PostStatus.DRAFT, "林默", null, 5,
            null, null, null, null, false
        ));

        assertEquals(1, revisions.countByPostId(saved.id()));
        assertEquals(1, revisions.findByPostIdOrderByVersionNoDesc(saved.id()).get(0).getVersionNo());
    }

    @Test
    void deletingAnArticleMovesItToRecycleBin() {
        var saved = service.save(null, new PostService.PostInput(
            "trash-test", "回收站文章", "摘要", "正文", null, "", "系统设计",
            Set.of(), PostStatus.DRAFT, "林默", null, 5,
            null, null, null, null, false
        ));

        service.delete(saved.id());

        Post deleted = posts.findById(saved.id()).orElseThrow();
        assertEquals(PostStatus.TRASHED, deleted.getStatus());
        assertNotNull(deleted.getDeletedAt());
    }

    @Test
    void missingArticleAuthorUsesConfiguredFallbackWhenNoAccountNameWasSubmitted() {
        SiteSetting author = settings.findById("authorName").orElseGet(SiteSetting::new);
        author.setKey("authorName");
        author.setValue("站点作者");
        settings.save(author);

        var saved = service.save(null, new PostService.PostInput(
            "configured-author", "使用站点作者", "摘要", "正文", null, "", "系统设计",
            Set.of(), PostStatus.DRAFT, "  ", null, 5,
            null, null, null, null, false
        ));

        assertEquals("站点作者", saved.authorName());
    }

    @Test
    void linkedArticleAlwaysUsesTheCurrentAccountIdentity() {
        var account = users.findByLoginName("admin").orElseThrow();
        String originalName = account.getName();
        try {
            account.setName("初始账户姓名");
            users.saveAndFlush(account);
            service.save(null, new PostService.PostInput(
                "linked-author", "账户身份同步", "摘要", "正文", null, "", "系统设计",
                Set.of(), PostStatus.PUBLISHED, "不应使用", null, 5,
                null, null, null, null, false
            ), "admin");

            account.setName("更新后的账户姓名");
            users.saveAndFlush(account);

            var published = service.published(null, null, null).stream()
                .filter(post -> "linked-author".equals(post.slug()))
                .findFirst()
                .orElseThrow();
            assertEquals("更新后的账户姓名", published.authorName());
        } finally {
            account.setName(originalName);
            users.saveAndFlush(account);
        }
    }

    @Test
    void bootstrapBindsLegacyArticlesWhenTheDeploymentDumpContainsNoUsers() throws Exception {
        var saved = service.save(null, new PostService.PostInput(
            "bootstrap-legacy-author", "首次部署历史文章", "摘要", "正文", null, "", "系统设计",
            Set.of(), PostStatus.PUBLISHED, "林默", null, 5,
            null, null, null, null, false
        ));
        assertNull(posts.findById(saved.id()).orElseThrow().getAuthor());

        new AdminUserBootstrap(users, posts, passwordEncoder, "fresh-admin", "fresh-admin-password").run(null);

        Post repaired = posts.findById(saved.id()).orElseThrow();
        assertNotNull(repaired.getAuthor());
        assertEquals("fresh-admin", repaired.getAuthor().getLoginName());
        assertEquals(repaired.getAuthor().getName(), repaired.getAuthorName());
    }
}
