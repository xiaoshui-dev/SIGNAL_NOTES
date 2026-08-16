package com.signalnotes.blog.service;

import com.signalnotes.blog.domain.SiteUser;
import com.signalnotes.blog.repository.PostRepository;
import com.signalnotes.blog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
public class AdminUserBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final PostRepository posts;
    private final PasswordEncoder encoder;
    private final String configuredUsername;
    private final String configuredPassword;

    public AdminUserBootstrap(UserRepository users, PostRepository posts, PasswordEncoder encoder,
                              @Value("${app.admin.username}") String configuredUsername,
                              @Value("${app.admin.password}") String configuredPassword) {
        this.users = users;
        this.posts = posts;
        this.encoder = encoder;
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = configuredUsername.trim();
        String email = username.contains("@") ? username : username + "@signal.local";
        SiteUser admin = users.findByLoginName(username)
            .or(() -> users.findByEmailIgnoreCase(email))
            .orElse(null);
        if (admin == null) {
            admin = new SiteUser();
            admin.setName("管理员");
            admin.setEmail(email);
            admin.setRole("ADMIN");
            admin.setStatus("ACTIVE");
        }
        if (admin.getName() == null || admin.getName().isBlank()) admin.setName("管理员");
        if (admin.getEmail() == null || admin.getEmail().isBlank()) admin.setEmail(email);
        admin.setLoginName(username);
        if (admin.getPasswordHash() == null || admin.getPasswordHash().isBlank()) {
            admin.setPasswordHash(encoder.encode(configuredPassword));
        }
        admin = users.save(admin);
        var legacyPosts = posts.findByAuthorIsNullAndAuthorNameIn(Set.of("林默", "站点作者"));
        for (var post : legacyPosts) {
            post.setAuthor(admin);
            post.setAuthorName(admin.getName());
        }
        posts.saveAll(legacyPosts);
    }
}
