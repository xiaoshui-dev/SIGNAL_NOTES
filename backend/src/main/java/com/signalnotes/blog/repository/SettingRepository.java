package com.signalnotes.blog.repository;
import com.signalnotes.blog.domain.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SettingRepository extends JpaRepository<SiteSetting, String> {}
