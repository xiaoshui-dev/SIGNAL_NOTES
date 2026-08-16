package com.signalnotes.blog.controller;

import com.signalnotes.blog.repository.SettingRepository;
import com.signalnotes.blog.service.SiteSettingPolicy;
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
        settings.findAll().forEach(item -> {
            if (SiteSettingPolicy.PUBLIC_KEYS.contains(item.getKey())) result.put(item.getKey(), item.getValue() == null ? "" : item.getValue());
        });
        return result;
    }
}
