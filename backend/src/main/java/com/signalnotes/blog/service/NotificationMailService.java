package com.signalnotes.blog.service;

import com.signalnotes.blog.domain.ContactMessage;
import com.signalnotes.blog.domain.Subscription;
import com.signalnotes.blog.repository.SettingRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Properties;

@Service
public class NotificationMailService {
    private static final Logger log = LoggerFactory.getLogger(NotificationMailService.class);
    private final SettingRepository settings;
    private final String publicUrl;

    public NotificationMailService(SettingRepository settings, @Value("${app.public-url:http://127.0.0.1:5174}") String publicUrl) {
        this.settings = settings;
        this.publicUrl = publicUrl.replaceAll("/$", "");
    }

    public boolean isConfigured() {
        return Boolean.parseBoolean(value("mail.enabled", "false"))
            && !value("mail.host", "").isBlank()
            && !value("mail.from", "").isBlank();
    }

    public boolean hasNotificationRecipient() {
        return !value("mail.notificationTo", "").isBlank();
    }

    public boolean sendSubscriptionConfirmation(Subscription subscription) {
        if (!isConfigured()) return false;
        String link = publicUrl + "/api/subscriptions/confirm?token=" + subscription.getConfirmationToken();
        return send(subscription.getEmail(), "确认你的 Signal Notes 订阅", "<p>你好，</p><p>请点击下面的链接确认订阅：</p><p><a href=\"" + link + "\">确认订阅</a></p><p>如果这不是你的操作，可以忽略这封邮件。</p>");
    }

    public boolean sendContactNotification(ContactMessage message) {
        if (!isConfigured() || !hasNotificationRecipient()) return false;
        String body = "<h2>新的联系反馈</h2><p><b>工单：</b>" + escape(message.getTicket()) + "</p><p><b>姓名：</b>" + escape(message.getName()) + "</p><p><b>邮箱：</b>" + escape(message.getEmail()) + "</p><p><b>主题：</b>" + escape(message.getSubject()) + "</p><p>" + escape(message.getMessage()).replace("\n", "<br>") + "</p>";
        return send(value("mail.notificationTo", ""), "Signal Notes 新反馈 · " + message.getTicket(), body);
    }

    public boolean sendTest(String recipient) {
        if (!isConfigured()) return false;
        return send(recipient, "Signal Notes 邮件配置测试", "<p>这是一封测试邮件。SMTP 配置已生效。</p>");
    }

    private boolean send(String recipient, String subject, String html) {
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(value("mail.host", ""));
            sender.setPort(Integer.parseInt(value("mail.port", "587")));
            sender.setUsername(value("mail.username", ""));
            sender.setPassword(value("mail.password", ""));
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", value("mail.auth", "true"));
            props.put("mail.smtp.starttls.enable", value("mail.starttls", "true"));
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(value("mail.from", ""));
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            return true;
        } catch (MessagingException | RuntimeException error) {
            log.error("SMTP delivery failed for recipient {}: {}", recipient, error.getMessage(), error);
            return false;
        }
    }

    private String value(String key, String fallback) {
        return settings.findById(key).map(item -> item.getValue() == null ? fallback : item.getValue()).orElse(fallback);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
