package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**") // Cho phép CORS cho tất cả các đường dẫn (API và image)
				.allowedOrigins("*") // Cho phép từ mọi nguồn
				.allowedMethods("GET", "POST", "PUT", "DELETE") // Các phương thức được phép
				.allowedHeaders("*"); // Cho phép tất cả các tiêu đề
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
	    // Sửa đường dẫn C:/ thành thư mục tương đối làm việc của Linux container[cite: 5]
	    String uploadDir = "file:" + System.getProperty("user.dir") + "/uploads/images/";
	    registry.addResourceHandler("/images/**").addResourceLocations(uploadDir);

	    String articleDir = "file:" + System.getProperty("user.dir") + "/uploads/articles/";
	    registry.addResourceHandler("/articles/**").addResourceLocations(articleDir);

	    registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
	}
}