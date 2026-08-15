package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

// Ép Java nạp trực tiếp bộ đọc/ghi WebP vào lõi ImageIO


@SpringBootApplication
@EnableScheduling
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ThuongMaiEurekaProductApplication {


	public static void main(String[] args) {
		
		SpringApplication.run(ThuongMaiEurekaProductApplication.class, args);

	}


}
