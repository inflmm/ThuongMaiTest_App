package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_session_logs", indexes = {
    // Đổi tên columnList sang đúng tên cột trong DB của BaseEntity (ví dụ: created_time)
    @Index(name = "idx_created_time", columnList = "created_time"),
    @Index(name = "idx_ip_address", columnList = "ip_address"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder // Dùng @SuperBuilder thay vì @Builder để kế thừa được các field từ BaseEntity
public class UserSessionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "ip_address", length = 45, nullable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "browser_name", length = 50)
    private String browserName;

    @Column(name = "os_name", length = 50)
    private String osName;

    @Column(name = "device_type", length = 30)
    private String deviceType;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Builder.Default
    @Column(name = "is_cron_ping")
    private Boolean isCronPing = false;

    // Đã bỏ createdAt & @PrePersist vì BaseEntity đã xử lý!
}