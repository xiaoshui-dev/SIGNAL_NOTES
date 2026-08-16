package com.signalnotes.blog.controller;

import com.signalnotes.blog.domain.ContactMessage;
import com.signalnotes.blog.repository.ContactMessageRepository;
import com.signalnotes.blog.service.NotificationMailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/contact")
public class ContactController {
    private final ContactMessageRepository messages;
    private final NotificationMailService mail;
    public ContactController(ContactMessageRepository messages, NotificationMailService mail) { this.messages = messages; this.mail = mail; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@Valid @RequestBody ContactMessage input, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (!input.isConsent()) throw new IllegalArgumentException("请先同意隐私说明");
        var duplicate = messages.findFirstByEmailAndMessageOrderByCreatedAtDesc(input.getEmail(), input.getMessage());
        if (duplicate.isPresent()) return Map.of("ticket", duplicate.get().getTicket(), "status", duplicate.get().getStatus(), "message", "这条内容已经收到");
        input.setId(null); input.setTicket("SN-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()); input.setStatus("RECEIVED");
        ContactMessage saved = messages.save(input);
        boolean notified = mail.sendContactNotification(saved);
        return Map.of("ticket", saved.getTicket(), "status", saved.getStatus(), "message", notified ? "反馈已收到，我们会尽快回复" : "反馈已收到；邮件提醒尚未配置，管理员可在后台查看");
    }
}
