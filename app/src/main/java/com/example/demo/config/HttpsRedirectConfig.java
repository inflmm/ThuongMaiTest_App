package com.example.demo.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpsRedirectConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                // Định nghĩa ràng buộc bảo mật (Security Constraint)
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL"); // Ép buộc bảo mật công khai
                
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*"); // Áp dụng cho toàn bộ các đường dẫn URL trên trang web
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        
        // Thêm cổng kết nối phụ (HTTP Connector) lắng nghe luồng traffic thường
        tomcat.addAdditionalTomcatConnectors(createHttpConnector());
        return tomcat;
    }

    private Connector createHttpConnector() {
        // Khởi tạo connector chạy giao thức HTTP chuẩn của Tomcat
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080); // Cổng HTTP thường mà người dùng hay gõ nhầm
        connector.setSecure(false);
        
        // Điều hướng toàn bộ request từ cổng 8080 sang cổng HTTPS 8443 bảo mật của bạn
        connector.setRedirectPort(8443); 
        return connector;
    }
}