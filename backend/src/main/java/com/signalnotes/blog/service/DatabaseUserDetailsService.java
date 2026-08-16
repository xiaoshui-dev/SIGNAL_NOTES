package com.signalnotes.blog.service;

import com.signalnotes.blog.domain.SiteUser;
import com.signalnotes.blog.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("users")
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SiteUser user = find(username);
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus()) || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new UsernameNotFoundException("用户未启用或尚未设置密码");
        }
        return User.withUsername(user.getLoginName()).password(user.getPasswordHash()).roles(role(user)).build();
    }

    public SiteUser find(String username) throws UsernameNotFoundException {
        String value = username == null ? "" : username.trim();
        return users.findByLoginName(value).or(() -> users.findByEmailIgnoreCase(value))
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
    }

    public void changePassword(String username, String passwordHash) {
        SiteUser user = find(username);
        user.setPasswordHash(passwordHash);
        users.save(user);
    }

    private String role(SiteUser user) {
        String value = user.getRole() == null ? "VIEWER" : user.getRole().trim().toUpperCase();
        return switch (value) {
            case "ADMIN", "EDITOR", "AUTHOR", "VIEWER" -> value;
            default -> "VIEWER";
        };
    }
}
