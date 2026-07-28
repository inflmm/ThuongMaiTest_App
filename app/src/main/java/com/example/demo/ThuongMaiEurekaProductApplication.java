package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

// Ép Java nạp trực tiếp bộ đọc/ghi WebP vào lõi ImageIO


@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ThuongMaiEurekaProductApplication {


	public static void main(String[] args) {
		
		SpringApplication.run(ThuongMaiEurekaProductApplication.class, args);

	}


}
