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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.Properties;

@Service
public class NotificationMailService {
    private static final int SMTP_TIMEOUT_MILLIS = 5000;
    private static final Set<String> MAIL_TRIMMED_KEYS = Set.of("mail.host", "mail.from", "mail.notificationTo", "mail.username", "mail.port");
    private static final Set<String> MAIL_BOOLEAN_KEYS = Set.of("mail.enabled", "mail.auth", "mail.starttls");
    private static final Logger log = LoggerFactory.getLogger(NotificationMailService.class);
    private final SettingRepository settings;
    private final String publicUrl;

    public NotificationMailService(SettingRepository settings, @Value("${app.public-url:http://127.0.0.1:5174}") String publicUrl) {
        this.settings = settings;
        this.publicUrl = publicUrl.replaceAll("/$", "");
    }

    public boolean isConfigured() {
        return configuredSnapshot() != null;
    }

    public boolean hasNotificationRecipient() {
        return isEmail(currentSettings().getOrDefault("mail.notificationTo", ""));
    }

    public Map<String, String> prepareSettings(Map<String, String> input) {
        Map<String, String> prepared = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (!"mail.passwordConfigured".equals(key)) prepared.put(key, normalizeValue(key, value));
        });
        Map<String, String> effective = currentSettings();
        prepared.forEach((key, value) -> {
            if (!("mail.password".equals(key) && value.isBlank())) effective.put(key, value);
        });
        String issue = configurationIssue(effective);
        if (issue != null) throw new IllegalArgumentException(issue);
        return prepared;
    }

    public void validateRecipient(String recipient) {
        if (!isEmail(recipient)) throw new IllegalArgumentException("请输入有效的测试邮箱");
    }

    public boolean sendSubscriptionConfirmation(Subscription subscription) {
        MailConfiguration config = configuredSnapshot();
        if (config == null) return false;
        String link = publicUrl + "/api/subscriptions/confirm?token=" + subscription.getConfirmationToken();
        return send(config, subscription.getEmail(), "确认你的 Signal Notes 订阅", "<p>你好，</p><p>请点击下面的链接确认订阅：</p><p><a href=\"" + link + "\">确认订阅</a></p><p>如果这不是你的操作，可以忽略这封邮件。</p>");
    }

    public boolean sendContactNotification(ContactMessage message) {
        MailConfiguration config = configuredSnapshot();
        if (config == null) return false;
        String body = "<h2>新的联系反馈</h2><p><b>工单：</b>" + escape(message.getTicket()) + "</p><p><b>姓名：</b>" + escape(message.getName()) + "</p><p><b>邮箱：</b>" + escape(message.getEmail()) + "</p><p><b>主题：</b>" + escape(message.getSubject()) + "</p><p>" + escape(message.getMessage()).replace("\n", "<br>") + "</p>";
        return send(config, config.notificationTo(), "Signal Notes 新反馈 · " + message.getTicket(), body);
    }

    public boolean sendTest(String recipient) {
        MailConfiguration config = configuredSnapshot();
        if (config == null) return false;
        return send(config, recipient, "Signal Notes 邮件配置测试", "<p>这是一封测试邮件。SMTP 配置已生效。</p>");
    }

    private boolean send(MailConfiguration config, String recipient, String subject, String html) {
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(config.host());
            sender.setPort(config.port());
            sender.setUsername(config.username());
            sender.setPassword(config.password());
            Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", Boolean.toString(config.auth()));
            props.put("mail.smtp.starttls.enable", Boolean.toString(config.starttls()));
            props.put("mail.smtp.connectiontimeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
            props.put("mail.smtp.timeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
            props.put("mail.smtp.writetimeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(config.from());
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

    private Map<String, String> currentSettings() {
        Map<String, String> result = new LinkedHashMap<>();
        settings.findAll().forEach(item -> result.put(item.getKey(), normalizeValue(item.getKey(), item.getValue())));
        return result;
    }

    private MailConfiguration configuredSnapshot() {
        Map<String, String> current = currentSettings();
        if (!Boolean.parseBoolean(current.getOrDefault("mail.enabled", "false")) || configurationIssue(current) != null) return null;
        return new MailConfiguration(current.getOrDefault("mail.host", ""), Integer.parseInt(current.get("mail.port")), current.getOrDefault("mail.from", ""),
            current.getOrDefault("mail.notificationTo", ""), current.getOrDefault("mail.username", ""), current.getOrDefault("mail.password", ""),
            Boolean.parseBoolean(current.getOrDefault("mail.auth", "true")), Boolean.parseBoolean(current.getOrDefault("mail.starttls", "true")));
    }

    private String normalizeValue(String key, String value) {
        String normalized = value == null ? "" : value;
        if (MAIL_BOOLEAN_KEYS.contains(key)) return normalized.trim().toLowerCase(Locale.ROOT);
        if (MAIL_TRIMMED_KEYS.contains(key)) return normalized.trim();
        return normalized;
    }

    private String configurationIssue(Map<String, String> values) {
        String enabled = values.getOrDefault("mail.enabled", "false");
        if (!isBoolean(enabled)) return "SMTP 启用状态必须为 true 或 false";
        if (!Boolean.parseBoolean(enabled)) return null;
        if (values.getOrDefault("mail.host", "").isBlank()) return "启用邮件服务前请填写 SMTP 主机";
        try {
            int port = Integer.parseInt(values.getOrDefault("mail.port", ""));
            if (port < 1 || port > 65535) return "SMTP 端口必须在 1-65535 之间";
        } catch (NumberFormatException error) {
            return "SMTP 端口必须是 1-65535 之间的数字";
        }
        if (!isEmail(values.getOrDefault("mail.from", ""))) return "请填写有效的 SMTP 发件人邮箱";
        if (!isEmail(values.getOrDefault("mail.notificationTo", ""))) return "请填写有效的反馈通知邮箱";
        String auth = values.getOrDefault("mail.auth", "true");
        String starttls = values.getOrDefault("mail.starttls", "true");
        if (!isBoolean(auth) || !isBoolean(starttls)) return "SMTP 登录和 STARTTLS 开关必须为 true 或 false";
        if (Boolean.parseBoolean(auth) && (values.getOrDefault("mail.username", "").isBlank() || values.getOrDefault("mail.password", "").isBlank())) {
            return "启用 SMTP 登录时必须填写用户名和密码";
        }
        return null;
    }

    private boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private boolean isEmail(String value) {
        return value != null && value.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private record MailConfiguration(String host, int port, String from, String notificationTo, String username, String password, boolean auth, boolean starttls) {}

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
