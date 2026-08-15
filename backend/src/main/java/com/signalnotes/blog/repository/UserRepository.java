package com.signalnotes.blog.repository;
import com.signalnotes.blog.domain.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<SiteUser, Long> {}
