package com.signalnotes.blog;

import com.signalnotes.blog.domain.*;
import com.signalnotes.blog.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest @AutoConfigureMockMvc
class ApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired PostRepository posts;
    @Autowired CategoryRepository categories;
    @Autowired CommentRepository comments;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired ContactMessageRepository contactMessages;
    @Autowired UserRepository siteUsers;
    @Autowired SettingRepository settings;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired MediaRepository mediaAssets;
    @Autowired com.signalnotes.blog.service.DatabaseUserDetailsService userDetails;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach void seed() {
        comments.deleteAll(); posts.deleteAll(); categories.deleteAll(); mediaAssets.deleteAll();
        Category category = new Category(); category.setName("系统设计"); category.setSlug("system-design"); category.setDescription("测试分类"); category = categories.save(category);
        Post post = new Post(); post.setSlug("test-post"); post.setTitle("测试文章"); post.setExcerpt("测试摘要"); post.setContent("## 正文"); post.setCategory(category); post.setStatus(PostStatus.PUBLISHED); post.setPublishedAt(LocalDate.now()); posts.save(post);
    }

    @Test void commentsSupportRepliesReportsAndSpamScreening() throws Exception {
        var approved = new Comment(); approved.setPostSlug("test-post"); approved.setAuthorName("读者"); approved.setContent("一条已通过的评论"); approved.setStatus(CommentStatus.APPROVED); approved = comments.save(approved);
        mvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON).content("{\"postSlug\":\"test-post\",\"parentId\":"+approved.getId()+",\"authorName\":\"回复者\",\"content\":\"感谢你的补充\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.parentId").value(approved.getId()));
        mvc.perform(post("/api/comments/{id}/report", approved.getId()).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"广告\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RECEIVED"));
        mvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON).content("{\"postSlug\":\"test-post\",\"authorName\":\"机器人\",\"content\":\"https://a.test https://b.test https://c.test 重复内容\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("SPAM"));
        var pending = new Comment(); pending.setPostSlug("test-post"); pending.setAuthorName("待审读者"); pending.setContent("等待管理员回复"); pending = comments.save(pending);
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        mvc.perform(post("/api/admin/comments/{id}/reply", pending.getId()).with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"管理员已回复\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.parentId").value(pending.getId()));
        Assertions.assertEquals(CommentStatus.APPROVED, comments.findById(pending.getId()).orElseThrow().getStatus());
    }

    @Test void mediaMetadataCanBeUpdatedAndReferencedAssetsCannotBeDeleted() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a,1,2,3,4});
        String response = mvc.perform(multipart("/api/admin/media").file(file).param("altText", "原始替代文本").with(auth)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var json = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(response);
        Long id = json.get("id").longValue();
        String url = json.get("url").asText();
        mvc.perform(patch("/api/admin/media/{id}", id).with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"filename\":\"updated.png\",\"altText\":\"更新后的替代文本\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.filename").value("updated.png"));
        MockMultipartFile replacement = new MockMultipartFile("file", "replacement.png", "image/png", new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a,5,6,7,8,9});
        mvc.perform(multipart("/api/admin/media/{id}/replace", id).file(replacement).with(auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.filename").value("replacement.png"))
            .andExpect(jsonPath("$.url").value(url));
        MockMultipartFile fake = new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1,2,3});
        mvc.perform(multipart("/api/admin/media").file(fake).with(auth)).andExpect(status().isBadRequest());
        Post post = posts.findBySlug("test-post").orElseThrow();
        post.setCover(url);
        posts.saveAndFlush(post);
        mvc.perform(get("/api/media")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].referenceCount").value(1))
            .andExpect(jsonPath("$[0].deletable").value(false));
        mvc.perform(delete("/api/admin/media/{id}", id).with(auth)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("1 篇文章")));
        post.setCover(null);
        posts.saveAndFlush(post);
        Path storedFile = Path.of("target/test-uploads", url.substring("/uploads/".length()));
        Assertions.assertTrue(Files.exists(storedFile));
        mvc.perform(delete("/api/admin/media/{id}", id).with(auth)).andExpect(status().isNoContent());
        Assertions.assertFalse(Files.exists(storedFile));
    }

    @Test void adminCanChangePasswordAndOldPasswordStopsWorking() throws Exception {
        var oldAuth=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin","signal2026");
        var newAuth=org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin","signal2026-new");
        try {
            mvc.perform(put("/api/admin/account/password").with(oldAuth).contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"signal2026\",\"newPassword\":\"signal2026-new\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UPDATED"));
            mvc.perform(get("/api/admin/dashboard").with(oldAuth)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/admin/dashboard").with(newAuth)).andExpect(status().isOk());
        } finally {
            userDetails.changePassword("admin", passwordEncoder.encode("signal2026"));
        }
    }

    @Test void publicPostApiReturnsPublishedContent() throws Exception {
        mvc.perform(get("/api/posts")).andExpect(status().isOk()).andExpect(jsonPath("$[0].slug").value("test-post"));
        mvc.perform(get("/api/posts/test-post")).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("测试文章"));
    }

    @Test void commentsAreCreatedPendingAndHiddenFromPublicList() throws Exception {
        mvc.perform(post("/api/comments").contentType(MediaType.APPLICATION_JSON).content("{\"postSlug\":\"test-post\",\"authorName\":\"读者\",\"content\":\"有帮助的文章\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));
        mvc.perform(get("/api/comments").param("postSlug", "test-post")).andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test void adminEndpointsRequireAuthentication() throws Exception {
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/dashboard").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test void viewerIsReadOnlyAcrossTheAdminApi() throws Exception {
        var viewer = user("viewer").roles("VIEWER");
        Long postId = posts.findBySlug("test-post").orElseThrow().getId();

        mvc.perform(get("/api/admin/dashboard").with(viewer)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/posts").with(viewer)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/posts").with(viewer).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/posts/{id}", postId).with(viewer))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/categories").with(viewer).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/contact-messages").with(viewer))
            .andExpect(status().isForbidden());
    }

    @Test void authorCanWriteArticlesButCannotPerformEditorialOrSystemActions() throws Exception {
        var author = user("author").roles("AUTHOR");
        Long postId = posts.findBySlug("test-post").orElseThrow().getId();
        String input = """
            {"slug":"author-post","title":"作者文章","excerpt":"摘要","content":"足够长的正文内容用于权限测试。足够长的正文内容用于权限测试。","category":"系统设计","tags":[],"status":"DRAFT","authorName":"作者","readMinutes":3}
            """;

        mvc.perform(post("/api/admin/posts").with(author).contentType(MediaType.APPLICATION_JSON).content(input))
            .andExpect(status().isCreated());
        mvc.perform(delete("/api/admin/posts/{id}", postId).with(author))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/categories").with(author).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/contact-messages").with(author))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/settings").with(author))
            .andExpect(status().isForbidden());
    }

    @Test void editorCanManageEditorialContentButNotSystemSettings() throws Exception {
        var editor = user("editor").roles("EDITOR");

        mvc.perform(post("/api/admin/categories").with(editor).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"编辑分类\",\"slug\":\"editor-category\",\"description\":\"权限测试\"}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/admin/contact-messages").with(editor))
            .andExpect(status().isOk());
        mvc.perform(put("/api/admin/settings").with(editor).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/backups").with(editor))
            .andExpect(status().isForbidden());
    }

    @Test void contactFormReturnsTicketAndRejectsOversizedMessage() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"读者\",\"email\":\"reader@example.com\",\"subject\":\"文章反馈\",\"message\":\"这是一条足够具体的反馈\",\"consent\":true}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.ticket").isString()).andExpect(jsonPath("$.status").value("RECEIVED"));
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"读者\",\"email\":\"reader@example.com\",\"subject\":\"文章反馈\",\"message\":\"" + "x".repeat(2001) + "\",\"consent\":true}"))
            .andExpect(status().isBadRequest());
    }

    @Test void contactIdempotencyKeyDeduplicatesOnlyExactRetries() throws Exception {
        long before = contactMessages.count();
        String payload = """
            {"name":"重复反馈者","email":"idempotency@example.com","subject":"重复问题","message":"相同内容可能在不同时间合法提交","consent":true}
            """;

        String first = mvc.perform(post("/api/contact").header("Idempotency-Key", "contact-attempt-1")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String retry = mvc.perform(post("/api/contact").header("Idempotency-Key", "contact-attempt-1")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String newAttempt = mvc.perform(post("/api/contact").header("Idempotency-Key", "contact-attempt-2")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isCreated());

        var mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder().build();
        Assertions.assertEquals(mapper.readTree(first).get("ticket").asText(), mapper.readTree(retry).get("ticket").asText());
        Assertions.assertNotEquals(mapper.readTree(first).get("ticket").asText(), mapper.readTree(newAttempt).get("ticket").asText());
        Assertions.assertEquals(before + 4, contactMessages.count());
    }

    @Test void adminCanListTaxonomyAndPostRevisions() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        mvc.perform(get("/api/admin/categories").with(auth)).andExpect(status().isOk());
        Long postId = posts.findBySlug("test-post").orElseThrow().getId();
        mvc.perform(get("/api/admin/posts/{id}/revisions", postId).with(auth)).andExpect(status().isOk());
    }

    @Test void publicTaxonomyAndSiteSettingsComeFromTheDatabase() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        String previousPassword = settings.findById("mail.password").map(SiteSetting::getValue).orElse("");
        var copyKeys = List.of("heroTitle", "aboutBody", "tagsIntro", "status503Description");
        var previousCopy = new java.util.LinkedHashMap<String, String>();
        copyKeys.forEach(key -> settings.findById(key).ifPresent(item -> previousCopy.put(key, item.getValue())));
        String previousShareTemplate = settings.findById("shareTemplate").map(SiteSetting::getValue).orElse(null);
        boolean hadShareTemplate = settings.existsById("shareTemplate");
        SiteSetting password = settings.findById("mail.password").orElseGet(SiteSetting::new);
        password.setKey("mail.password");
        password.setValue("qa-task6-public-api-secret");
        settings.save(password);
        try {
            mvc.perform(put("/api/admin/settings").with(auth).contentType(MediaType.APPLICATION_JSON).content("""
                    {"heroTitle":"qa-task6-hero","aboutBody":"qa-task6-about","tagsIntro":"qa-task6-tags","status503Description":"qa-task6-status","shareTemplate":"portrait","mail.passwordConfigured":"true"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heroTitle").value("qa-task6-hero"))
                .andExpect(jsonPath("$.aboutBody").value("qa-task6-about"))
                .andExpect(jsonPath("$.shareTemplate").value("portrait"));
            mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("系统设计"));
            mvc.perform(get("/api/tags")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
            mvc.perform(get("/api/site")).andExpect(status().isOk())
                .andExpect(jsonPath("$.heroTitle").value("qa-task6-hero"))
                .andExpect(jsonPath("$.aboutBody").value("qa-task6-about"))
                .andExpect(jsonPath("$.tagsIntro").value("qa-task6-tags"))
                .andExpect(jsonPath("$.status503Description").value("qa-task6-status"))
                .andExpect(jsonPath("$.shareTemplate").value("portrait"))
                .andExpect(jsonPath("$['mail.password']").doesNotExist())
                .andExpect(jsonPath("$['mail.enabled']").doesNotExist());
        } finally {
            copyKeys.forEach(key -> {
                if (!previousCopy.containsKey(key)) {
                    settings.deleteById(key);
                    return;
                }
                SiteSetting item = settings.findById(key).orElseGet(SiteSetting::new);
                item.setKey(key);
                item.setValue(previousCopy.get(key));
                settings.save(item);
            });
            password.setValue(previousPassword);
            settings.save(password);
            if (hadShareTemplate) {
                SiteSetting item = settings.findById("shareTemplate").orElseGet(SiteSetting::new);
                item.setKey("shareTemplate"); item.setValue(previousShareTemplate); settings.save(item);
            } else settings.deleteById("shareTemplate");
        }
    }

    @Test void v9SiteCopyMigrationIsIdempotentAndPreservesEditedValues() throws Exception {
        String table = "site_settings_migration_test";
        jdbc.execute("CREATE TABLE " + table + " (setting_key VARCHAR(100) PRIMARY KEY, setting_value CLOB)");
        try {
            String sql = Files.readString(Path.of("src/main/resources/db/migration/V9__complete_site_copy_defaults.sql")).replace("site_settings", table);
            jdbc.execute(sql);
            jdbc.update("UPDATE " + table + " SET setting_value = ? WHERE setting_key = ?", "qa-task6-preserved", "heroTitle");
            jdbc.execute(sql);
            Assertions.assertEquals("qa-task6-preserved", jdbc.queryForObject("SELECT setting_value FROM " + table + " WHERE setting_key = 'heroTitle'", String.class));
            Assertions.assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class) >= 80);
        } finally {
            jdbc.execute("DROP TABLE " + table);
        }
    }

    @Test void publicSiteRejectsUnknownWritesAndHidesOperationalRows() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        String previousSecret = settings.findById("ops.internal.secret").map(SiteSetting::getValue).orElse(null);
        String previousHeroSummary = settings.findById("heroSummary").map(SiteSetting::getValue).orElse(null);
        boolean hadSecret = settings.existsById("ops.internal.secret");
        boolean hadHeroSummary = settings.existsById("heroSummary");
        SiteSetting secret = new SiteSetting();
        secret.setKey("ops.internal.secret");
        secret.setValue("qa-task6-secret");
        SiteSetting nullable = new SiteSetting();
        nullable.setKey("heroSummary");
        nullable.setValue(null);
        settings.saveAll(List.of(secret, nullable));
        try {
            mvc.perform(put("/api/admin/settings").with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"ops.internal.secret\":\"rejected\"}"))
                .andExpect(status().isBadRequest());
            mvc.perform(get("/api/site")).andExpect(status().isOk())
                .andExpect(jsonPath("$.heroSummary").value(""))
                .andExpect(jsonPath("$['ops.internal.secret']").doesNotExist());
        } finally {
            if (hadSecret) { secret.setValue(previousSecret); settings.save(secret); } else settings.deleteById("ops.internal.secret");
            if (hadHeroSummary) { nullable.setValue(previousHeroSummary); settings.save(nullable); } else settings.deleteById("heroSummary");
        }
    }

    @Test void fullControlledSettingsPayloadIncludingShareAndMailSettingsIsAccepted() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        var payload = new java.util.LinkedHashMap<String, String>();
        com.signalnotes.blog.service.SiteSettingPolicy.PUBLIC_KEYS.forEach(key -> payload.put(key, ""));
        payload.put("heroTitle", "qa-task6-full-settings");
        payload.put("shareTemplate", "portrait");
        com.signalnotes.blog.service.SiteSettingPolicy.MAIL_KEYS.forEach(key -> payload.put(key, key.equals("mail.enabled") ? "false" : ""));
        payload.put("mail.passwordConfigured", "true");
        var previous = new java.util.LinkedHashMap<String, String>();
        payload.keySet().forEach(key -> settings.findById(key).ifPresent(item -> previous.put(key, item.getValue())));
        try {
            mvc.perform(put("/api/admin/settings").with(auth).contentType(MediaType.APPLICATION_JSON)
                    .content(com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(payload)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.heroTitle").value("qa-task6-full-settings"))
                .andExpect(jsonPath("$.shareTemplate").value("portrait"));
            var oversized = new java.util.LinkedHashMap<String, String>();
            for (int index = 0; index <= com.signalnotes.blog.service.SiteSettingPolicy.MAX_KEYS; index++) oversized.put("qa-unknown-" + index, "x");
            mvc.perform(put("/api/admin/settings").with(auth).contentType(MediaType.APPLICATION_JSON)
                    .content(com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(oversized)))
                .andExpect(status().isBadRequest());
            var oversizedValue = java.util.Map.of("heroTitle", "x".repeat(com.signalnotes.blog.service.SiteSettingPolicy.MAX_VALUE_LENGTH + 1));
            mvc.perform(put("/api/admin/settings").with(auth).contentType(MediaType.APPLICATION_JSON)
                    .content(com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(oversizedValue)))
                .andExpect(status().isBadRequest());
            var oversizedDerived = java.util.Map.of("mail.passwordConfigured", "x".repeat(com.signalnotes.blog.service.SiteSettingPolicy.MAX_VALUE_LENGTH + 1));
            mvc.perform(put("/api/admin/settings").with(auth).contentType(MediaType.APPLICATION_JSON)
                    .content(com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(oversizedDerived)))
                .andExpect(status().isBadRequest());
            var oversizedAggregate = new java.util.LinkedHashMap<String, String>();
            com.signalnotes.blog.service.SiteSettingPolicy.PUBLIC_KEYS.stream().limit(16)
                    .forEach(key -> oversizedAggregate.put(key, "x".repeat(com.signalnotes.blog.service.SiteSettingPolicy.MAX_VALUE_LENGTH)));
            mvc.perform(put("/api/admin/settings").with(auth).contentType(MediaType.APPLICATION_JSON)
                    .content(com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(oversizedAggregate)))
                .andExpect(status().isBadRequest());
        } finally {
            payload.keySet().forEach(key -> {
                if ("mail.passwordConfigured".equals(key)) return;
                if (previous.containsKey(key)) {
                    SiteSetting item = settings.findById(key).orElseGet(SiteSetting::new);
                    item.setKey(key); item.setValue(previous.get(key)); settings.save(item);
                } else settings.deleteById(key);
            });
        }
    }

    @Test void trashedPostCanBeRestoredAndPermanentlyDeleted() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        Long id = posts.findBySlug("test-post").orElseThrow().getId();
        mvc.perform(delete("/api/admin/posts/{id}", id).with(auth)).andExpect(status().isNoContent());
        mvc.perform(get("/api/posts/test-post")).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/posts/{id}/restore", id).with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
        mvc.perform(delete("/api/admin/posts/{id}", id).with(auth)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/admin/posts/{id}?permanent=true", id).with(auth)).andExpect(status().isNoContent());
        Assertions.assertFalse(posts.existsById(id));
    }

    @Test void subscriptionsAndContactMessagesAreVisibleToAdministrators() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        mvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"subscriber@example.com\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("邮件服务尚未配置")));
        mvc.perform(get("/api/admin/subscriptions").with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$[0].email").value("subscriber@example.com"));
        mvc.perform(post("/api/admin/email/test").with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"owner@example.com\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(false)).andExpect(jsonPath("$.sent").value(false));
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"读者\",\"email\":\"inbox@example.com\",\"subject\":\"后台反馈\",\"message\":\"这是一条应该进入后台收件箱的内容\",\"consent\":true}"))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/admin/contact-messages").with(auth)).andExpect(status().isOk()).andExpect(jsonPath("$[0].subject").value("后台反馈"));
        Long id = contactMessages.findAll().stream().filter(item -> item.getEmail().equals("inbox@example.com")).findFirst().orElseThrow().getId();
        mvc.perform(patch("/api/admin/contact-messages/{id}", id).with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test void administratorsCanCreateUpdateAndDeleteSiteUsers() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        String body = mvc.perform(post("/api/admin/users").with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"新作者\",\"email\":\"new-author@example.com\",\"role\":\"AUTHOR\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("INVITED")).andReturn().getResponse().getContentAsString();
        Long id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(body).get("id").longValue();
        mvc.perform(put("/api/admin/users/{id}", id).with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"新作者 2\",\"loginName\":\"new-author\",\"password\":\"author-password-123\",\"role\":\"EDITOR\",\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("EDITOR"));
        mvc.perform(delete("/api/admin/users/{id}", id).with(auth)).andExpect(status().isNoContent());
    }

    @Test void managedActiveUserCanAuthenticateWithDatabaseCredentials() throws Exception {
        var admin = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        mvc.perform(post("/api/admin/users").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"内容编辑\",\"email\":\"writer@example.com\",\"loginName\":\"writer\",\"password\":\"writer-password-123\",\"role\":\"EDITOR\",\"status\":\"ACTIVE\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.loginName").value("writer"));
        var writer = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("writer", "writer-password-123");
        mvc.perform(get("/api/admin/dashboard").with(writer)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/me").with(writer)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("EDITOR"));
        mvc.perform(get("/api/admin/settings").with(writer)).andExpect(status().isForbidden());
        Long id = siteUsers.findAll().stream().filter(item -> "writer".equals(item.getLoginName())).findFirst().orElseThrow().getId();
        mvc.perform(put("/api/admin/users/{id}", id).with(admin).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DISABLED\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/admin/dashboard").with(writer)).andExpect(status().isUnauthorized());
    }

    @Test void smtpPasswordIsMaskedAndBlankUpdatesPreserveTheStoredSecret() throws Exception {
        var admin = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        SiteSetting password = new SiteSetting();
        password.setKey("mail.password");
        password.setValue("smtp-secret-value");
        settings.save(password);
        mvc.perform(get("/api/admin/settings").with(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['mail.password']").value(""))
            .andExpect(jsonPath("$['mail.passwordConfigured']").value("true"));
        mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"mail.password\":\"\",\"siteName\":\"保留密码测试\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$['mail.password']").value(""));
        Assertions.assertEquals("smtp-secret-value", settings.findById("mail.password").orElseThrow().getValue());
    }

    @Test void smtpSettingsAreValidatedAndFailedNotificationsStayInTheInbox() throws Exception {
        var admin = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        try {
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\"false\"}"))
                .andExpect(status().isOk());
            mvc.perform(post("/api/admin/email/test").with(admin).contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("有效的测试邮箱")));
            mvc.perform(post("/api/admin/email/test").with(admin).contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"owner@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.sent").value(false)).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("尚未配置")));
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\"sometimes\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("启用状态")));
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\"true\",\"mail.host\":\"\",\"mail.port\":\"587\",\"mail.from\":\"owner@example.com\",\"mail.notificationTo\":\"owner@example.com\",\"mail.auth\":\"false\",\"mail.starttls\":\"true\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("SMTP 主机")));
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\"true\",\"mail.host\":\"smtp.example.com\",\"mail.port\":\"70000\",\"mail.from\":\"owner@example.com\",\"mail.notificationTo\":\"owner@example.com\",\"mail.auth\":\"false\",\"mail.starttls\":\"true\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("端口")));
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\"true\",\"mail.host\":\"smtp.example.com\",\"mail.port\":\"587\",\"mail.from\":\"invalid\",\"mail.notificationTo\":\"owner@example.com\",\"mail.auth\":\"false\",\"mail.starttls\":\"true\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("发件人")));
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\"true\",\"mail.host\":\"smtp.example.com\",\"mail.port\":\"587\",\"mail.from\":\"owner@example.com\",\"mail.notificationTo\":\"invalid\",\"mail.auth\":\"false\",\"mail.starttls\":\"true\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("反馈通知邮箱")));
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\"true\",\"mail.host\":\"smtp.example.com\",\"mail.port\":\"587\",\"mail.from\":\"owner@example.com\",\"mail.notificationTo\":\"owner@example.com\",\"mail.auth\":\"maybe\",\"mail.starttls\":\"true\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("开关")));
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\"true\",\"mail.host\":\"smtp.example.com\",\"mail.port\":\"587\",\"mail.from\":\"owner@example.com\",\"mail.notificationTo\":\"owner@example.com\",\"mail.auth\":\"false\",\"mail.starttls\":\"maybe\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("STARTTLS")));
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\"true\",\"mail.host\":\"smtp.example.com\",\"mail.port\":\"587\",\"mail.from\":\"owner@example.com\",\"mail.notificationTo\":\"owner@example.com\",\"mail.auth\":\"true\",\"mail.starttls\":\"true\",\"mail.username\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("用户名和密码")));
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\"true\",\"mail.host\":\"127.0.0.1\",\"mail.port\":\"1\",\"mail.from\":\"owner@example.com\",\"mail.notificationTo\":\"owner@example.com\",\"mail.auth\":\"false\",\"mail.starttls\":\"false\"}"))
                .andExpect(status().isOk());
            mvc.perform(post("/api/subscriptions").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"qa-task5-subscription@example.com\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("发送失败")));
            Assertions.assertTrue(subscriptions.findByEmailIgnoreCase("qa-task5-subscription@example.com").isPresent());
            mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"邮件失败读者\",\"email\":\"qa-task5-feedback@example.com\",\"subject\":\"邮件失败仍需入库\",\"message\":\"这条反馈必须保存在收件箱里，即使 SMTP 连接失败\",\"consent\":true}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("发送失败")));
            Assertions.assertTrue(contactMessages.findAll().stream().anyMatch(item -> "qa-task5-feedback@example.com".equals(item.getEmail())));
            mvc.perform(post("/api/admin/email/test").with(admin).contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"owner@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.sent").value(false)).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("发送失败")));
        } finally {
            SiteSetting enabled = settings.findById("mail.enabled").orElseGet(SiteSetting::new);
            enabled.setKey("mail.enabled");
            enabled.setValue("false");
            settings.save(enabled);
        }
    }

    @Test void smtpSettingsAreNormalizedAndSavedTransactionally() throws Exception {
        var admin = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        try {
            mvc.perform(put("/api/admin/settings").with(admin).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mail.enabled\":\" TRUE \",\"mail.host\":\" smtp.example.com \",\"mail.port\":\" 587 \",\"mail.from\":\" sender@example.com \",\"mail.notificationTo\":\" owner@example.com \",\"mail.auth\":\" TRUE \",\"mail.starttls\":\" FALSE \",\"mail.username\":\" smtp-user \",\"mail.password\":\"  password with spaces  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['mail.password']").value(""))
                .andExpect(jsonPath("$['mail.passwordConfigured']").value("true"));
            Assertions.assertEquals("true", settings.findById("mail.enabled").orElseThrow().getValue());
            Assertions.assertEquals("smtp.example.com", settings.findById("mail.host").orElseThrow().getValue());
            Assertions.assertEquals("587", settings.findById("mail.port").orElseThrow().getValue());
            Assertions.assertEquals("sender@example.com", settings.findById("mail.from").orElseThrow().getValue());
            Assertions.assertEquals("owner@example.com", settings.findById("mail.notificationTo").orElseThrow().getValue());
            Assertions.assertEquals("true", settings.findById("mail.auth").orElseThrow().getValue());
            Assertions.assertEquals("false", settings.findById("mail.starttls").orElseThrow().getValue());
            Assertions.assertEquals("smtp-user", settings.findById("mail.username").orElseThrow().getValue());
            Assertions.assertEquals("  password with spaces  ", settings.findById("mail.password").orElseThrow().getValue());
            var method = com.signalnotes.blog.controller.AdminController.class.getDeclaredMethod("saveSettings", java.util.Map.class, org.springframework.security.core.Authentication.class);
            Assertions.assertNotNull(method.getAnnotation(Transactional.class));
        } finally {
            SiteSetting enabled = settings.findById("mail.enabled").orElseGet(SiteSetting::new);
            enabled.setKey("mail.enabled");
            enabled.setValue("false");
            settings.save(enabled);
        }
    }

    @Test void smtpSendUsesOneConfigurationSnapshotAndFiniteTimeouts() {
        SettingRepository repository = org.mockito.Mockito.mock(SettingRepository.class);
        org.mockito.Mockito.when(repository.findAll()).thenReturn(List.of(
            setting("mail.enabled", "true"), setting("mail.host", "smtp.example.com"), setting("mail.port", "587"),
            setting("mail.from", "sender@example.com"), setting("mail.notificationTo", "owner@example.com"),
            setting("mail.auth", "true"), setting("mail.starttls", "true"), setting("mail.username", "smtp-user"),
            setting("mail.password", "password with spaces")
        ));
        var service = new com.signalnotes.blog.service.NotificationMailService(repository, "http://127.0.0.1:5174");
        Properties properties = new Properties();
        try (var ignored = org.mockito.Mockito.mockConstruction(org.springframework.mail.javamail.JavaMailSenderImpl.class,
                (sender, context) -> {
                    org.mockito.Mockito.when(sender.getJavaMailProperties()).thenReturn(properties);
                    org.mockito.Mockito.when(sender.createMimeMessage()).thenReturn(new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties())));
                })) {
            Assertions.assertTrue(service.sendTest("reader@example.com"));
        }
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(1)).findAll();
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).findById(org.mockito.ArgumentMatchers.anyString());
        Assertions.assertEquals("5000", properties.getProperty("mail.smtp.connectiontimeout"));
        Assertions.assertEquals("5000", properties.getProperty("mail.smtp.timeout"));
        Assertions.assertEquals("5000", properties.getProperty("mail.smtp.writetimeout"));
    }

    private SiteSetting setting(String key, String value) {
        SiteSetting setting = new SiteSetting();
        setting.setKey(key);
        setting.setValue(value);
        return setting;
    }

    @Test void backupCreatesVerifiedArtifactVisibleInTaskList() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        SiteSetting password = new SiteSetting();
        password.setKey("mail.password");
        password.setValue("backup-secret-value");
        settings.save(password);
        mvc.perform(post("/api/admin/backups").with(auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true)).andExpect(jsonPath("$.checksum").isString());
        String filename = mvc.perform(get("/api/admin/backups").with(auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("VERIFIED")).andReturn().getResponse().getContentAsString();
        String latest = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(filename).get(0).get("filename").asText();
        Assertions.assertFalse(Files.readString(Path.of("target/test-backups", latest)).contains("backup-secret-value"));
    }
}
