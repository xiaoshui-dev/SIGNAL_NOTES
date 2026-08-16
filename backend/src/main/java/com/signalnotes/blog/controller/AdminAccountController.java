package com.signalnotes.blog.controller;

import com.signalnotes.blog.service.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.signalnotes.blog.service.DatabaseUserDetailsService;
import com.signalnotes.blog.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/admin/account")
public class AdminAccountController {
    private final DatabaseUserDetailsService users; private final UserRepository repository; private final PasswordEncoder encoder; private final AuditService audit;
    public AdminAccountController(DatabaseUserDetailsService users,UserRepository repository,PasswordEncoder encoder,AuditService audit){this.users=users;this.repository=repository;this.encoder=encoder;this.audit=audit;}
    @PutMapping("/profile") public Map<String,Object> updateProfile(@Valid @RequestBody ProfileUpdate input,Authentication auth){var user=repository.findByLoginName(auth.getName()).orElseThrow(()->new EntityNotFoundException("用户不存在"));String avatar=input.avatarUrl()==null?null:input.avatarUrl().trim();if(avatar!=null&&!avatar.isBlank()&&!avatar.startsWith("/uploads/")&&!avatar.startsWith("https://"))throw new IllegalArgumentException("头像必须来自媒体库或 HTTPS 地址");user.setName(input.name().trim());user.setAvatarUrl(avatar==null||avatar.isBlank()?null:avatar);repository.save(user);audit.log(auth.getName(),"UPDATE_ACCOUNT_PROFILE",user.getId().toString());Map<String,Object> value=new java.util.LinkedHashMap<>();value.put("id",user.getId());value.put("loginName",user.getLoginName());value.put("name",user.getName());value.put("avatarUrl",user.getAvatarUrl());value.put("role",user.getRole());value.put("status",user.getStatus());return value;}
    @PutMapping("/password") public Map<String,String> changePassword(@Valid @RequestBody PasswordChange input,Authentication auth){var current=users.loadUserByUsername(auth.getName());if(!encoder.matches(input.currentPassword(),current.getPassword()))throw new IllegalArgumentException("当前密码不正确");if(input.currentPassword().equals(input.newPassword()))throw new IllegalArgumentException("新密码不能与当前密码相同");users.changePassword(auth.getName(),encoder.encode(input.newPassword()));audit.log(auth.getName(),"CHANGE_PASSWORD",auth.getName());return Map.of("status","UPDATED","message","密码已更新，请使用新密码重新登录");}
    public record PasswordChange(@NotBlank String currentPassword,@NotBlank @Size(min=10,max=128) String newPassword){}
    public record ProfileUpdate(@NotBlank @Size(max=80) String name,@Size(max=500) String avatarUrl){}
}
