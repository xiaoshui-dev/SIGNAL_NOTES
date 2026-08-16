package com.signalnotes.blog.repository;
import com.signalnotes.blog.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByEmailIgnoreCase(String email);
    Optional<Subscription> findByConfirmationToken(String token);
    List<Subscription> findAllByOrderByCreatedAtDesc();
}
