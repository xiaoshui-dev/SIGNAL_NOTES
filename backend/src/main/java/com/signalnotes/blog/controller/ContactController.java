package com.signalnotes.blog.controller;

import com.signalnotes.blog.domain.ContactMessage;
import com.signalnotes.blog.repository.ContactMessageRepository;
import com.signalnotes.blog.service.NotificationMailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
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
        String key = normalizeIdempotencyKey(idempotencyKey);
        if (key != null) {
            var duplicate = messages.findByIdempotencyKey(key);
            if (duplicate.isPresent()) return duplicateResponse(duplicate.get());
        }
        input.setId(null); input.setIdempotencyKey(key); input.setTicket("SN-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()); input.setStatus("RECEIVED");
        ContactMessage saved;
        try {
            saved = messages.saveAndFlush(input);
        } catch (DataIntegrityViolationException error) {
            if (key != null) return messages.findByIdempotencyKey(key).map(this::duplicateResponse).orElseThrow(() -> error);
            throw error;
        }
        boolean notified = mail.sendContactNotification(saved);
        String message = notified ? "反馈已收到，我们会尽快回复"
            : mail.isConfigured() && mail.hasNotificationRecipient() ? "反馈已收到；提醒邮件发送失败，管理员仍可在后台查看"
            : "反馈已收到；邮件提醒尚未配置，管理员可在后台查看";
        return Map.of("ticket", saved.getTicket(), "status", saved.getStatus(), "message", message);
    }

    private String normalizeIdempotencyKey(String value) {
        if (value == null || value.isBlank()) return null;
        String key = value.trim();
        if (key.length() > 100) throw new IllegalArgumentException("Idempotency-Key 不能超过 100 个字符");
        return key;
    }

    private Map<String, String> duplicateResponse(ContactMessage message) {
        return Map.of("ticket", message.getTicket(), "status", message.getStatus(), "message", "这次反馈已经收到");
    }
}
