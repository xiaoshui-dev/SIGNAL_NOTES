package com.signalnotes.blog.repository;

import com.signalnotes.blog.domain.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    Optional<ContactMessage> findByIdempotencyKey(String idempotencyKey);
}
