package com.example.demo.controller;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.demo.dto.ProductDetailDto;
import com.example.demo.model.Blog;
import com.example.demo.service.BlogService;
import com.example.demo.service.ProductService;

@Controller
public class WebController {

	@Autowired
    private ProductService productService;

	@Autowired
	private BlogService blogService;

    @GetMapping("/homepage") // Đường dẫn bạn sẽ gõ trên trình duyệt
    public String getHomePage() {
        return "homepage"; // Tên file HTML (không có đuôi .html)
    }

    @GetMapping("/products/{slug}")
    public String getProductBySlug(@PathVariable("slug") String slug, Model model) {
        // Gọi hàm mới trả về Optional<ProductDetailDto>
        Optional<ProductDetailDto> productDtoOpt = productService.getProductDetailBySlug(slug);

        if (productDtoOpt.isEmpty()) {
            return "404";
        }

        // Đưa DTO đã có đầy đủ masterFiles vào Model
        model.addAttribute("product", productDtoOpt.get());
        return "product-detail";
    }
    
    @GetMapping("/products-backup/{slug}")
    public String getProductBySlugBackup(@PathVariable("slug") String slug, Model model) {
        // Gọi hàm mới trả về Optional<ProductDetailDto>
        Optional<ProductDetailDto> productDtoOpt = productService.getProductDetailBySlug(slug);

        if (productDtoOpt.isEmpty()) {
            return "404";
        }

        // Đưa DTO đã có đầy đủ masterFiles vào Model
        model.addAttribute("product", productDtoOpt.get());
        return "product-detail-backup";
    }

    @GetMapping("/success")
    public String getSuccess() {
    	return "success";
    }

    @GetMapping("/cart")
    public String getCart() {
    	return "cart";
    }

    @GetMapping("/collections")
    public String getCollections() {
    	return "collections";
    }

 // 1. Đường dẫn trang danh sách bài viết
    @GetMapping("/blogs/all")
    public String getAllBlogs(Model model) {
        // Lấy Optional từ service
        Optional<List<Blog>> blogsOpt = blogService.getAllPublicBlogs();

        // Nếu có dữ liệu thì gửi list, nếu không gửi list rỗng để tránh lỗi th:each
        model.addAttribute("blogs", blogsOpt.orElse(Collections.emptyList()));

        return "blog-all";
    }

    // 2. Đường dẫn trang chi tiết bài viết
    @GetMapping("/blogs/{slug}")
    public String getBlogBySlug(@PathVariable("slug") String slug, Model model) {
        Optional<Blog> blogOpt = blogService.getPublicBlogBySlug(slug);

        if (blogOpt.isEmpty()) {
            return "404";
        }

        model.addAttribute("blog", blogOpt.get());
        return "blog-detail"; // Trả về file blog-detail.html
    }

    @GetMapping("/blogs/api/{slug}")
    public String getApiBlogBySlug(@PathVariable("slug") String slug, Model model) {
        Optional<Blog> blogOpt = blogService.getPublicBlogBySlug(slug);

        if (blogOpt.isEmpty()) {
            return "404";
        }

        //model.addAttribute("blog", blogOpt.get());
        return "blog-detail-api-fetch"; // Trả về file blog-detail.html
    }

    @GetMapping({"/admin", "/admin/main/**"})
    public String adminMainPage() {
        return "admin/admin-main"; // Luôn trả về file main cho mọi sub-path
    }

    @GetMapping("/admin/login")
    public String adminLoginPage() {
        return "admin/admin-login";
    }
}

