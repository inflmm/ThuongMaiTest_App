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
		// Ánh xạ đường dẫn URL "/images/**" tới thư mục vật lý vd:
		// "file:///C:/ecommerce-uploads/images/"
		// Lưu ý: Cần có dấu '/' ở cuối
		String imagePath = "file:///C:/ecommerce-uploads/images/";
		registry.addResourceHandler("/images/**").addResourceLocations(imagePath);

		// 2. Đảm bảo các file tĩnh trong project vẫn hoạt động bình thường
		registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");

		registry.addResourceHandler("/articles/**").addResourceLocations("file:///C:/ecommerce-uploads/articles/");
	}
}