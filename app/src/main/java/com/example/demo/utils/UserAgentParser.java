package com.example.demo.utils;

public class UserAgentParser {

    public static class UserAgentInfo {
        private final String browserName;
        private final String osName;
        private final String deviceType;

        public UserAgentInfo(String browserName, String osName, String deviceType) {
            this.browserName = browserName;
            this.osName = osName;
            this.deviceType = deviceType;
        }

        public String getBrowserName() {
            return browserName;
        }

        public String getOsName() {
            return osName;
        }

        public String getDeviceType() {
            return deviceType;
        }
    }

    public static UserAgentInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new UserAgentInfo("Unknown", "Unknown", "Unknown");
        }

        if (userAgent.contains("Go-http-client")) {
            return new UserAgentInfo("Go-http-client", "Server", "Bot");
        }

        String ua = userAgent.toLowerCase();

        String deviceType = "Desktop";
        if (ua.contains("tablet") || ua.contains("ipad") || ua.contains("nexus 7") || ua.contains("nexus 9") || ua.contains("kindle") || ua.contains("silk")) {
            deviceType = "Tablet";
        } else if (ua.contains("mobi") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipod")) {
            deviceType = "Mobile";
        }

        String osName = "Unknown OS";
        if (ua.contains("windows")) {
            osName = "Windows";
        } else if (ua.contains("mac os x") || ua.contains("macintosh")) {
            osName = "Mac OS X";
        } else if (ua.contains("android")) {
            osName = "Android";
        } else if (ua.contains("linux")) {
            osName = "Linux";
        } else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            osName = "iOS";
        }

        String browserName = "Other Browser";
        if (ua.contains("edg/") || ua.contains("edge/")) {
            browserName = "Microsoft Edge";
        } else if (ua.contains("chrome/") && !ua.contains("edg/") && !ua.contains("opr/")) {
            browserName = "Google Chrome";
        } else if (ua.contains("safari/") && !ua.contains("chrome/")) {
            browserName = "Safari";
        } else if (ua.contains("firefox/")) {
            browserName = "Mozilla Firefox";
        } else if (ua.contains("opr/") || ua.contains("opera/")) {
            browserName = "Opera";
        } else if (ua.contains("msie") || ua.contains("trident/")) {
            browserName = "Internet Explorer";
        }

        return new UserAgentInfo(browserName, osName, deviceType);
    }
}
