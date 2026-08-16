package com.signalnotes.blog.controller;

import com.signalnotes.blog.repository.SettingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SiteController {
    private final SettingRepository settings;

    public SiteController(SettingRepository settings) { this.settings = settings; }

    @GetMapping("/api/site")
    public Map<String, String> site() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("siteName", "脉冲笔记");
        result.put("siteShortName", "SIGNAL NOTES");
        result.put("siteTagline", "把复杂技术讲清楚");
        result.put("heroEyebrow", "TECH NOTES / 2026");
        result.put("heroTitle", "把复杂技术讲清楚");
        result.put("heroSummary", "关于 AI、软件工程、系统与数字世界的长期记录。少一点噪声，多一点真正有用的理解。");
        result.put("aboutTitle", "写给愿意慢下来理解技术的人。");
        result.put("aboutLead", "脉冲笔记关注技术背后的结构、取舍和真实影响。");
        result.put("aboutBody", "这里没有追逐热点的速报，只有经过实践、验证和反思之后的记录。");
        result.put("contactTitle", "把问题、想法或合作方向告诉我。");
        result.put("contactIntro", "如果你发现文章中的错误，或者有值得长期讨论的技术问题，欢迎留下消息。");
        result.put("publicEmail", "hello@signal-notes.local");
        result.put("replyPromise", "通常会在 3 个工作日内回复。");
        result.put("privacyContent", "我们只收集完成联系、评论和订阅所必需的信息，不出售个人数据。");
        result.put("footerDescription", "关于 AI、系统与数字世界的独立技术博客。");
        result.put("copyrightText", "© 2026 Signal Notes");
        result.put("licenseText", "内容以 CC BY-NC-SA 4.0 发布");
        result.put("subscribeTitle", "每两周，收到一封有用的信。");
        result.put("subscribeDescription", "只发送新文章和真正值得分享的链接，不追踪打开行为。");
        settings.findAll().forEach(item -> { if (!item.getKey().startsWith("mail.")) result.put(item.getKey(), item.getValue()); });
        return result;
    }
}
