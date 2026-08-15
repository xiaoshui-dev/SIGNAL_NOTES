package com.signalnotes.blog.repository;
import com.signalnotes.blog.domain.MediaAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MediaRepository extends JpaRepository<MediaAsset, Long> { List<MediaAsset> findAllByOrderByCreatedAtDesc(); }
