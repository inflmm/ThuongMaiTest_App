package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_session_logs", indexes = {
    @Index(name = "idx_created_time", columnList = "created_time"),
    @Index(name = "idx_ip_address", columnList = "ip_address"),
    @Index(name = "idx_user_id", columnList = "user_id")
})
public class UserSessionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 100)
    private String userId;

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

    @Column(name = "is_cron_ping")
    private Boolean isCronPing = false;

    public UserSessionLog() {
    }

    public UserSessionLog(Long id, String userId, String username, String ipAddress, String userAgent,
                          String browserName, String osName, String deviceType, String sessionId, Boolean isCronPing) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.browserName = browserName;
        this.osName = osName;
        this.deviceType = deviceType;
        this.sessionId = sessionId;
        this.isCronPing = isCronPing;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String userId;
        private String username;
        private String ipAddress;
        private String userAgent;
        private String browserName;
        private String osName;
        private String deviceType;
        private String sessionId;
        private Boolean isCronPing = false;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder browserName(String browserName) {
            this.browserName = browserName;
            return this;
        }

        public Builder osName(String osName) {
            this.osName = osName;
            return this;
        }

        public Builder deviceType(String deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder isCronPing(Boolean isCronPing) {
            this.isCronPing = isCronPing;
            return this;
        }

        public UserSessionLog build() {
            return new UserSessionLog(
                id, userId, username, ipAddress, userAgent,
                browserName, osName, deviceType, sessionId, isCronPing
            );
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getBrowserName() {
        return browserName;
    }

    public void setBrowserName(String browserName) {
        this.browserName = browserName;
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Boolean getIsCronPing() {
        return isCronPing;
    }

    public void setIsCronPing(Boolean isCronPing) {
        this.isCronPing = isCronPing;
    }
}