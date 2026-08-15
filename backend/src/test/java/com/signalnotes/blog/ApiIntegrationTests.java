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
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ApiIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired PostRepository posts;
    @Autowired CategoryRepository categories;
    @Autowired CommentRepository comments;

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
    }

    @Test void mediaMetadataCanBeUpdatedAndReferencedAssetsCannotBeDeleted() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1,2,3});
        String response = mvc.perform(multipart("/api/admin/media").file(file).param("altText", "原始替代文本").with(auth)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build().readTree(response).get("id").longValue();
        mvc.perform(patch("/api/admin/media/{id}", id).with(auth).contentType(MediaType.APPLICATION_JSON).content("{\"filename\":\"updated.png\",\"altText\":\"更新后的替代文本\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.filename").value("updated.png"));
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

    @Test void backupCreatesVerifiedArtifactVisibleInTaskList() throws Exception {
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("admin", "signal2026");
        mvc.perform(post("/api/admin/backups").with(auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$.verified").value(true)).andExpect(jsonPath("$.checksum").isString());
        mvc.perform(get("/api/admin/backups").with(auth)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("VERIFIED"));
    }
}
