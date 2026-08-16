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
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @Test void contactFormReturnsTicketAndRejectsOversizedMessage() throws Exception {
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"读者\",\"email\":\"reader@example.com\",\"subject\":\"文章反馈\",\"message\":\"这是一条足够具体的反馈\",\"consent\":true}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.ticket").isString()).andExpect(jsonPath("$.status").value("RECEIVED"));
        mvc.perform(post("/api/contact").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"读者\",\"email\":\"reader@example.com\",\"subject\":\"文章反馈\",\"message\":\"" + "x".repeat(2001) + "\",\"consent\":true}"))
            .andExpect(status().isBadRequest());
    }

    @Test void adminCanListTaxonomyAndPostRevisions() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        mvc.perform(get("/api/admin/categories").with(auth)).andExpect(status().isOk());
        Long postId = posts.findBySlug("test-post").orElseThrow().getId();
        mvc.perform(get("/api/admin/posts/{id}/revisions", postId).with(auth)).andExpect(status().isOk());
    }

    @Test void publicTaxonomyAndSiteSettingsComeFromTheDatabase() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        mvc.perform(put("/api/admin/settings").with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"heroTitle\":\"数据库里的标题\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("系统设计"));
        mvc.perform(get("/api/tags")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/site")).andExpect(status().isOk())
            .andExpect(jsonPath("$.heroTitle").value("数据库里的标题"))
            .andExpect(jsonPath("$.searchTitle").isNotEmpty())
            .andExpect(jsonPath("$.searchIntro").isNotEmpty())
            .andExpect(jsonPath("$.categoriesTitle").isNotEmpty())
            .andExpect(jsonPath("$.categoriesIntro").isNotEmpty())
            .andExpect(jsonPath("$.aboutPrinciple1Title").isNotEmpty())
            .andExpect(jsonPath("$.aboutPrinciple1Body").isNotEmpty());
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
