package com.signalnotes.blog.controller;

import com.signalnotes.blog.domain.Subscription;
import com.signalnotes.blog.repository.SubscriptionRepository;
import com.signalnotes.blog.service.NotificationMailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.time.Instant;
import java.util.UUID;

@RestController @RequestMapping("/api/subscriptions")
public class SubscriptionController {
    private final SubscriptionRepository subscriptions;
    private final NotificationMailService mail;
    public SubscriptionController(SubscriptionRepository subscriptions, NotificationMailService mail) { this.subscriptions = subscriptions; this.mail = mail; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> subscribe(@Valid @RequestBody SubscribeRequest request) {
        Subscription value = subscriptions.findByEmailIgnoreCase(request.email()).orElseGet(Subscription::new);
        value.setEmail(request.email().toLowerCase());
        value.setUnsubscribedAt(null);
        if (!"CONFIRMED".equals(value.getStatus())) {
            value.setStatus("PENDING");
            value.setConfirmationToken(UUID.randomUUID().toString().replace("-", ""));
        }
        value.setUpdatedAt(Instant.now());
        value = subscriptions.save(value);
        boolean sent = "PENDING".equals(value.getStatus()) && mail.sendSubscriptionConfirmation(value);
        return Map.of("status", value.getStatus(), "mailConfigured", mail.isConfigured(), "message", sent ? "确认邮件已发送，请查收" : mail.isConfigured() ? "订阅已保存，但确认邮件发送失败，请联系管理员" : "订阅已保存，邮件服务尚未配置，管理员可在后台处理");
    }

    @GetMapping("/confirm")
    public Map<String, String> confirm(@RequestParam String token) {
        Subscription value = subscriptions.findByConfirmationToken(token).orElseThrow(() -> new IllegalArgumentException("确认链接无效或已失效"));
        value.setStatus("CONFIRMED"); value.setConfirmedAt(Instant.now()); value.setUpdatedAt(Instant.now()); subscriptions.save(value);
        return Map.of("status", "CONFIRMED", "message", "订阅已确认");
    }

    @GetMapping("/unsubscribe")
    public Map<String, String> unsubscribe(@RequestParam String token) {
        Subscription value = subscriptions.findByConfirmationToken(token).orElseThrow(() -> new IllegalArgumentException("退订链接无效或已失效"));
        value.setStatus("UNSUBSCRIBED"); value.setUnsubscribedAt(Instant.now()); value.setUpdatedAt(Instant.now()); subscriptions.save(value);
        return Map.of("status", "UNSUBSCRIBED", "message", "已取消订阅");
    }
    public record SubscribeRequest(@NotBlank @Email String email) {}
}
