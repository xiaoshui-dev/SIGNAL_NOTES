package com.signalnotes.blog.repository;
import com.signalnotes.blog.domain.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<SiteUser, Long> {
    Optional<SiteUser> findByLoginName(String loginName);
    Optional<SiteUser> findByEmailIgnoreCase(String email);
    boolean existsByAvatarUrl(String avatarUrl);
}
