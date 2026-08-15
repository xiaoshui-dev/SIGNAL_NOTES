package com.signalnotes.blog.repository;
import com.signalnotes.blog.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CommentRepository extends JpaRepository<Comment, Long> { List<Comment> findByPostSlugAndStatusOrderByCreatedAtDesc(String postSlug, CommentStatus status); List<Comment> findAllByOrderByCreatedAtDesc(); long countByStatus(CommentStatus status); }
