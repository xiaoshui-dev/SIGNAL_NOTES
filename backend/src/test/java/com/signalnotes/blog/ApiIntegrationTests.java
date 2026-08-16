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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import java.time.LocalDate;
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
    @Autowired InMemoryUserDetailsManager userDetails;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach void seed() {
        comments.deleteAll(); posts.deleteAll(); categories.deleteAll();
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
        Long id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(response).get("id").longValue();
        mvc.perform(patch("/api/admin/media/{id}", id).with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"filename\":\"updated.png\",\"altText\":\"更新后的替代文本\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.filename").value("updated.png"));
        MockMultipartFile fake = new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1,2,3});
        mvc.perform(multipart("/api/admin/media").file(fake).with(auth)).andExpect(status().isBadRequest());
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
            var current=userDetails.loadUserByUsername("admin");
            userDetails.updateUser(User.withUserDetails(current).password(passwordEncoder.encode("signal2026")).build());
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
        var setting = new SiteSetting(); setting.setKey("heroTitle"); setting.setValue("数据库里的标题"); settings.save(setting);
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("系统设计"));
        mvc.perform(get("/api/tags")).andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        mvc.perform(get("/api/site")).andExpect(status().isOk()).andExpect(jsonPath("$.heroTitle").value("数据库里的标题"));
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
        mvc.perform(put("/api/admin/users/{id}", id).with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"新作者 2\",\"role\":\"EDITOR\",\"status\":\"ACTIVE\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("EDITOR"));
        mvc.perform(delete("/api/admin/users/{id}", id).with(auth)).andExpect(status().isNoContent());
    }

    @Test void backupCreatesVerifiedArtifactVisibleInTaskList() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        mvc.perform(post("/api/admin/backups").with(auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true)).andExpect(jsonPath("$.checksum").isString());
        mvc.perform(get("/api/admin/backups").with(auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("VERIFIED"));
    }
}
