package com.signalnotes.blog.repository;

import com.signalnotes.blog.domain.PostRevision;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PostRevisionRepository extends JpaRepository<PostRevision, Long> {
    long countByPostId(Long postId);
    List<PostRevision> findByPostIdOrderByVersionNoDesc(Long postId);
    Optional<PostRevision> findTopByPostIdOrderByVersionNoDesc(Long postId);
}
