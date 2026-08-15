package com.signalnotes.blog.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "site_settings") @Getter @Setter @NoArgsConstructor
public class SiteSetting {
    @Id @Column(name = "setting_key", length = 100) private String key;
    @Lob @Column(name = "setting_value", columnDefinition = "TEXT") private String value;
}
