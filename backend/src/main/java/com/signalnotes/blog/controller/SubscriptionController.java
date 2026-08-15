package com.signalnotes.blog.controller;

import com.signalnotes.blog.domain.Subscription;
import com.signalnotes.blog.repository.SubscriptionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/subscriptions")
public class SubscriptionController {
    private final SubscriptionRepository subscriptions;
    public SubscriptionController(SubscriptionRepository subscriptions) { this.subscriptions = subscriptions; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> subscribe(@Valid @RequestBody SubscribeRequest request) {
        Subscription value = subscriptions.findByEmailIgnoreCase(request.email()).orElseGet(Subscription::new); value.setEmail(request.email().toLowerCase()); subscriptions.save(value);
        return Map.of("status", value.getStatus(), "message", "请查收确认邮件");
    }
    public record SubscribeRequest(@NotBlank @Email String email) {}
}
