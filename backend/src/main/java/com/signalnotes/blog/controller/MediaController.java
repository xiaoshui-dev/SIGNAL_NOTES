package com.signalnotes.blog.controller;

import com.signalnotes.blog.domain.MediaAsset;
import com.signalnotes.blog.repository.MediaRepository;
import com.signalnotes.blog.service.AuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
public class MediaController {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
    private final MediaRepository media; private final AuditService audit; private final Path uploadDir;
    public MediaController(MediaRepository media, AuditService audit, @Value("${app.upload-dir}") String uploadDir) { this.media=media;this.audit=audit;this.uploadDir=Path.of(uploadDir).toAbsolutePath().normalize(); }
    @GetMapping("/api/media") public List<MediaAsset> list(){return media.findAllByOrderByCreatedAtDesc();}
    @PostMapping(value="/api/admin/media",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public MediaAsset upload(@RequestPart("file") MultipartFile file,@RequestPart(value="altText",required=false) String altText,Authentication auth) throws IOException {
        if(file.isEmpty()||file.getSize()>10*1024*1024)throw new IllegalArgumentException("文件为空或超过 10MB");
        if(!ALLOWED.contains(file.getContentType()))throw new IllegalArgumentException("只支持 JPG、PNG 和 WebP");
        Files.createDirectories(uploadDir);String extension=switch(file.getContentType()){case "image/png"->".png";case "image/webp"->".webp";default->".jpg";};String stored=UUID.randomUUID()+extension;Path target=uploadDir.resolve(stored).normalize();if(!target.startsWith(uploadDir))throw new IllegalArgumentException("非法文件路径");file.transferTo(target);
        MediaAsset asset=new MediaAsset();asset.setFilename(file.getOriginalFilename()==null?stored:Path.of(file.getOriginalFilename()).getFileName().toString());asset.setUrl("/uploads/"+stored);asset.setMimeType(file.getContentType());asset.setSize(file.getSize());asset.setAltText(altText);asset=media.save(asset);audit.log(auth.getName(),"UPLOAD_MEDIA",asset.getFilename());return asset;
    }
    @DeleteMapping("/api/admin/media/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,Authentication auth) throws IOException {MediaAsset asset=media.findById(id).orElseThrow(()->new jakarta.persistence.EntityNotFoundException("媒体不存在"));Path file=uploadDir.resolve(asset.getUrl().replace("/uploads/","")).normalize();if(file.startsWith(uploadDir))Files.deleteIfExists(file);media.delete(asset);audit.log(auth.getName(),"DELETE_MEDIA",id.toString());}
}
