package com.signalnotes.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.signalnotes.blog.domain.BackupJob;
import com.signalnotes.blog.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class BackupService {
    private final BackupJobRepository jobs; private final PostService posts; private final CommentRepository comments; private final SettingRepository settings; private final ObjectMapper json; private final Path directory;
    public BackupService(BackupJobRepository jobs, PostService posts, CommentRepository comments, SettingRepository settings, ObjectMapper json, @Value("${app.backup-dir:./backups}") String directory) { this.jobs=jobs;this.posts=posts;this.comments=comments;this.settings=settings;this.json=json;this.directory=Path.of(directory).toAbsolutePath().normalize(); }

    public BackupJob create() {
        try {
            Files.createDirectories(directory);
            String filename = "signal-notes-" + Instant.now().toEpochMilli() + ".json";
            BackupJob job = new BackupJob(); job.setFilename(filename); job = jobs.save(job);
            Map<String,Object> payload = new LinkedHashMap<>(); payload.put("formatVersion",1); payload.put("createdAt",Instant.now()); payload.put("posts",posts.all()); payload.put("comments",comments.findAll()); payload.put("settings",settings.findAll());
            byte[] bytes = json.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload); Path target = directory.resolve(filename).normalize(); if(!target.startsWith(directory))throw new IllegalStateException("非法备份路径"); Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
            job.setSize((long)bytes.length); job.setChecksum(hex(MessageDigest.getInstance("SHA-256").digest(bytes))); job.setVerified(Arrays.equals(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(target)),MessageDigest.getInstance("SHA-256").digest(bytes))); job.setStatus(job.isVerified()?"VERIFIED":"FAILED"); job.setVerifiedAt(job.isVerified()?Instant.now():null); return jobs.save(job);
        } catch (Exception error) { throw new IllegalStateException("备份失败：" + error.getMessage(), error); }
    }

    public List<BackupJob> list() { return jobs.findTop20ByOrderByCreatedAtDesc(); }
    private String hex(byte[] bytes) { return java.util.HexFormat.of().formatHex(bytes); }
}
