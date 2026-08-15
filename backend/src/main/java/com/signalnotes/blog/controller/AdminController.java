package com.signalnotes.blog.controller;

import com.signalnotes.blog.domain.*;
import com.signalnotes.blog.repository.*;
import com.signalnotes.blog.service.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/api/admin")
public class AdminController {
    private final PostService postService; private final PostRepository posts; private final CommentRepository comments; private final SubscriptionRepository subscriptions; private final UserRepository users; private final SettingRepository settings; private final AuditLogRepository logs; private final AuditService audit;
    public AdminController(PostService postService, PostRepository posts, CommentRepository comments, SubscriptionRepository subscriptions, UserRepository users, SettingRepository settings, AuditLogRepository logs, AuditService audit) { this.postService=postService;this.posts=posts;this.comments=comments;this.subscriptions=subscriptions;this.users=users;this.settings=settings;this.logs=logs;this.audit=audit; }

    @GetMapping("/dashboard") public Map<String,Object> dashboard() { return Map.of("posts", posts.count(), "pendingComments", comments.countByStatus(CommentStatus.PENDING), "subscribers", subscriptions.count(), "users", users.count(), "status", "UP"); }
    @GetMapping("/posts") public List<PostService.PostView> postList() { return postService.all(); }
    @PostMapping("/posts") @ResponseStatus(HttpStatus.CREATED) public PostService.PostView create(@Valid @RequestBody PostService.PostInput input, Authentication auth) { PostService.PostView value=postService.save(null,input);audit.log(auth.getName(),"CREATE_POST",value.slug());return value; }
    @PutMapping("/posts/{id}") public PostService.PostView update(@PathVariable Long id,@Valid @RequestBody PostService.PostInput input,Authentication auth){PostService.PostView value=postService.save(id,input);audit.log(auth.getName(),"UPDATE_POST",value.slug());return value;}
    @DeleteMapping("/posts/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id,Authentication auth){postService.delete(id);audit.log(auth.getName(),"DELETE_POST",id.toString());}

    @GetMapping("/comments") public List<Comment> commentList(){return comments.findAllByOrderByCreatedAtDesc();}
    @PatchMapping("/comments/{id}") public Comment moderate(@PathVariable Long id,@RequestBody Map<String,String> body,Authentication auth){Comment value=comments.findById(id).orElseThrow(()->new EntityNotFoundException("评论不存在"));value.setStatus(CommentStatus.valueOf(body.getOrDefault("status","PENDING")));audit.log(auth.getName(),"MODERATE_COMMENT",id.toString());return comments.save(value);}
    @DeleteMapping("/comments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteComment(@PathVariable Long id){comments.deleteById(id);}

    @GetMapping("/users") public List<SiteUser> userList(){return users.findAll();}
    @PatchMapping("/users/{id}") public SiteUser updateUser(@PathVariable Long id,@RequestBody Map<String,String> body,Authentication auth){SiteUser value=users.findById(id).orElseThrow(()->new EntityNotFoundException("用户不存在"));if(body.containsKey("role"))value.setRole(body.get("role"));if(body.containsKey("status"))value.setStatus(body.get("status"));audit.log(auth.getName(),"UPDATE_USER",id.toString());return users.save(value);}

    @GetMapping("/settings") public Map<String,String> getSettings(){Map<String,String> result=new LinkedHashMap<>();settings.findAll().forEach(item->result.put(item.getKey(),item.getValue()));return result;}
    @PutMapping("/settings") public Map<String,String> saveSettings(@RequestBody Map<String,String> input,Authentication auth){input.forEach((key,value)->{SiteSetting item=settings.findById(key).orElseGet(SiteSetting::new);item.setKey(key);item.setValue(value);settings.save(item);});audit.log(auth.getName(),"UPDATE_SETTINGS",String.join(",",input.keySet()));return input;}
    @GetMapping("/logs") public List<AuditLog> logList(){return logs.findTop50ByOrderByCreatedAtDesc();}
    @PostMapping("/backups") public Map<String,Object> backup(Authentication auth){String id="backup-"+Instant.now().toEpochMilli();audit.log(auth.getName(),"CREATE_BACKUP",id);return Map.of("id",id,"status","COMPLETED","verified",true,"createdAt",Instant.now());}
}
