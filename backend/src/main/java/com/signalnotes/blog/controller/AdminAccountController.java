package com.signalnotes.blog.controller;

import com.signalnotes.blog.service.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.signalnotes.blog.service.DatabaseUserDetailsService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/admin/account")
public class AdminAccountController {
    private final DatabaseUserDetailsService users; private final PasswordEncoder encoder; private final AuditService audit;
    public AdminAccountController(DatabaseUserDetailsService users,PasswordEncoder encoder,AuditService audit){this.users=users;this.encoder=encoder;this.audit=audit;}
    @PutMapping("/password") public Map<String,String> changePassword(@Valid @RequestBody PasswordChange input,Authentication auth){var current=users.loadUserByUsername(auth.getName());if(!encoder.matches(input.currentPassword(),current.getPassword()))throw new IllegalArgumentException("当前密码不正确");if(input.currentPassword().equals(input.newPassword()))throw new IllegalArgumentException("新密码不能与当前密码相同");users.changePassword(auth.getName(),encoder.encode(input.newPassword()));audit.log(auth.getName(),"CHANGE_PASSWORD",auth.getName());return Map.of("status","UPDATED","message","密码已更新，请使用新密码重新登录");}
    public record PasswordChange(@NotBlank String currentPassword,@NotBlank @Size(min=10,max=128) String newPassword){}
}
