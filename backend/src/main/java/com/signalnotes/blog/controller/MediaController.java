package com.signalnotes.blog.controller;

import com.signalnotes.blog.domain.MediaAsset;
import com.signalnotes.blog.repository.MediaRepository;
import com.signalnotes.blog.repository.PostRepository;
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
    private final MediaRepository media; private final PostRepository posts; private final AuditService audit; private final Path uploadDir;
    public MediaController(MediaRepository media, PostRepository posts, AuditService audit, @Value("${app.upload-dir}") String uploadDir) { this.media=media;this.posts=posts;this.audit=audit;this.uploadDir=Path.of(uploadDir).toAbsolutePath().normalize(); }
    @GetMapping("/api/media") public List<MediaAsset> list(){return media.findAllByOrderByCreatedAtDesc();}
    @PostMapping(value="/api/admin/media",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED)
    public MediaAsset upload(@RequestPart("file") MultipartFile file,@RequestPart(value="altText",required=false) String altText,Authentication auth) throws IOException {
        if(file.isEmpty()||file.getSize()>10*1024*1024)throw new IllegalArgumentException("文件为空或超过 10MB");
        if(!ALLOWED.contains(file.getContentType()))throw new IllegalArgumentException("只支持 JPG、PNG 和 WebP");
        validateSignature(file);
        Files.createDirectories(uploadDir);String extension=switch(file.getContentType()){case "image/png"->".png";case "image/webp"->".webp";default->".jpg";};String stored=UUID.randomUUID()+extension;Path target=uploadDir.resolve(stored).normalize();if(!target.startsWith(uploadDir))throw new IllegalArgumentException("非法文件路径");file.transferTo(target);
        MediaAsset asset=new MediaAsset();asset.setFilename(file.getOriginalFilename()==null?stored:Path.of(file.getOriginalFilename()).getFileName().toString());asset.setUrl("/uploads/"+stored);asset.setMimeType(file.getContentType());asset.setSize(file.getSize());asset.setAltText(altText);asset=media.save(asset);audit.log(auth.getName(),"UPLOAD_MEDIA",asset.getFilename());return asset;
    }
    @DeleteMapping("/api/admin/media/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,Authentication auth) throws IOException {MediaAsset asset=media.findById(id).orElseThrow(()->new jakarta.persistence.EntityNotFoundException("媒体不存在"));if(posts.findAll().stream().anyMatch(post->asset.getUrl().equals(post.getCover())))throw new IllegalArgumentException("媒体仍被文章引用，请先替换封面");Path file=uploadDir.resolve(asset.getUrl().replace("/uploads/","")).normalize();if(file.startsWith(uploadDir))Files.deleteIfExists(file);media.delete(asset);audit.log(auth.getName(),"DELETE_MEDIA",id.toString());}
    @PatchMapping("/api/admin/media/{id}") public MediaAsset update(@PathVariable Long id,@RequestBody Map<String,String> body,Authentication auth){MediaAsset asset=media.findById(id).orElseThrow(()->new jakarta.persistence.EntityNotFoundException("媒体不存在"));if(body.containsKey("filename")&&!body.get("filename").isBlank())asset.setFilename(Path.of(body.get("filename")).getFileName().toString());if(body.containsKey("altText"))asset.setAltText(body.get("altText"));MediaAsset value=media.save(asset);audit.log(auth.getName(),"UPDATE_MEDIA",id.toString());return value;}
    @PostMapping(value="/api/admin/media/{id}/replace",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public MediaAsset replace(@PathVariable Long id,@RequestPart("file") MultipartFile file,Authentication auth) throws IOException {MediaAsset asset=media.findById(id).orElseThrow(()->new jakarta.persistence.EntityNotFoundException("媒体不存在"));if(file.isEmpty()||file.getSize()>10*1024*1024||!ALLOWED.contains(file.getContentType()))throw new IllegalArgumentException("文件为空、超过 10MB 或类型不支持");if(!asset.getMimeType().equals(file.getContentType()))throw new IllegalArgumentException("替换文件需保持原图片格式");validateSignature(file);Path target=uploadDir.resolve(asset.getUrl().replace("/uploads/","")).normalize();if(!target.startsWith(uploadDir))throw new IllegalArgumentException("非法文件路径");file.transferTo(target);asset.setSize(file.getSize());if(file.getOriginalFilename()!=null)asset.setFilename(Path.of(file.getOriginalFilename()).getFileName().toString());MediaAsset value=media.save(asset);audit.log(auth.getName(),"REPLACE_MEDIA",id.toString());return value;}
    private void validateSignature(MultipartFile file) throws IOException {byte[] h=file.getInputStream().readNBytes(12);boolean valid=switch(file.getContentType()){case "image/jpeg"->h.length>=3&&(h[0]&255)==0xff&&(h[1]&255)==0xd8&&(h[2]&255)==0xff;case "image/png"->h.length>=8&&(h[0]&255)==0x89&&h[1]==0x50&&h[2]==0x4e&&h[3]==0x47&&h[4]==0x0d&&h[5]==0x0a&&h[6]==0x1a&&h[7]==0x0a;case "image/webp"->h.length>=12&&new String(h,0,4,java.nio.charset.StandardCharsets.US_ASCII).equals("RIFF")&&new String(h,8,4,java.nio.charset.StandardCharsets.US_ASCII).equals("WEBP");default->false;};if(!valid)throw new IllegalArgumentException("文件内容与声明的图片类型不一致");}
}
