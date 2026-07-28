package com.example.demo.service;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.ProductDetailDto;
import com.example.demo.model.Product;
import com.example.demo.model.ProductSpecification;
import com.example.demo.repository.ProductRepository;

import jakarta.transaction.Transactional;

@Service // Đánh dấu đây là Service Component
public class ProductService {

    // Dependency Injection: Tự động kết nối với ProductRepository
    @Autowired
    private ProductRepository productRepository;

    /**
     * Lấy tất cả sản phẩm CHƯA BỊ XÓA (Active Products)
     * @return Danh sách các Product đang hoạt động
     */
    public List<Product> getAllActiveProducts() {
        // Sử dụng phương thức Soft Delete đã định nghĩa trong Repository
        return productRepository.findByDeletedFalse();
    }

    /**
     * Lấy thông tin chi tiết một sản phẩm theo ID (chỉ khi chưa bị xóa)
     * @param id ID của sản phẩm
     * @return Optional<Product> (chứa sản phẩm hoặc rỗng nếu không tìm thấy)
     */
    public Optional<Product> getProductBySlug(String slug) {
        // Sử dụng phương thức Soft Delete đã định nghĩa trong Repository
        return productRepository.findBySlugAndDeletedFalse(slug);
    }

    @Transactional // Quan trọng: Đảm bảo lưu cả cụm hoặc không gì cả
    public Product saveProduct(Product product) {
        // 1. Tạo Slug nếu chưa có
        if (product.getSlug() == null || product.getSlug().isEmpty()) {
            product.setSlug(generateSlug(product.getName()));
        }

        // 3. Thiết lập liên kết ngược cho Thông số kỹ thuật
        if (product.getSpecifications() != null) {
            product.getSpecifications().forEach(spec -> spec.setProduct(product));
        }

        return productRepository.save(product);
    }

    /**
     * Xóa mềm (Soft Delete) một sản phẩm
     * @param id ID của sản phẩm cần xóa
     */
    public void softDeleteProduct(String slug) {
        // 1. Tìm sản phẩm
        Optional<Product> productOpt = productRepository.findBySlug(slug);

        // 2. Nếu tìm thấy, cập nhật trường 'deleted' thành true
        productOpt.ifPresent(product -> {
            product.setDeleted(true);
            productRepository.save(product);
        });

        // (Nếu không tìm thấy, có thể throw exception, nhưng ở đây ta bỏ qua đơn giản)
    }

    public String generateSlug(String name) {
        String noAccent = Normalizer.normalize(name, Normalizer.Form.NFD)
                                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return noAccent.toLowerCase()
                       .replaceAll("[^a-z0-9\\s]", "")
                       .replaceAll("\\s+", "-")
                       .replaceAll("^-+|-+$", "");
    }

    public List<Product> getProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        // Sử dụng hàm có sẵn của JPA
        return productRepository.findByIdInAndDeletedFalse(ids);
    }

    public Optional<ProductDetailDto> getProductDetailBySlug(String slug) {
        // 1. Tìm Product Entity (đang trả về Optional<Product>)
        return productRepository.findBySlugAndDeletedFalse(slug)
                .map(p -> {
                    // 2. Nếu có Product, dùng hàm mapping hiện tại của bạn
                    ProductDetailDto dto = getProductDetailDto(p);

                    // 3. Nạp danh sách ảnh vào trường masterFiles từ DTO
                    dto.setMasterFiles(getMasterImages(p.getImage_folder_path()));

                    return dto;
                });
    }

    // Hàm mapping cũ của bạn (giữ nguyên logic, chỉ đảm bảo nó được gọi bên trong map)
    private ProductDetailDto getProductDetailDto(Product p) {
        ProductDetailDto dto = new ProductDetailDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setPrice(p.getPrice());
        dto.setImageUrl(p.getImageUrl());
        dto.setImage_folder_path(p.getImage_folder_path());
        dto.setLongDescription(p.getLongDescription());

        // Logic map specifications giữ nguyên
        if (p.getSpecifications() != null) {
            dto.setSpecifications(p.getSpecifications().stream()
                    .collect(Collectors.toMap(
                        ProductSpecification::getSpecKey,
                        ProductSpecification::getSpecValue,
                        (existing, replacement) -> existing // Tránh lỗi duplicate key
                    )));
        }
        return dto;
    }
    public List<String> getMasterImages(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) {
			return new ArrayList<>();
		}

        String rootPath = "C:\\ecommerce-uploads\\";
        File folder = new File(rootPath, folderPath + "/master");

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                System.out.println("Total files found: " + files.length);
                return Arrays.stream(files)
                        .filter(f -> {
                            boolean match = f.isFile() && f.getName().toLowerCase().endsWith("_master.webp");
                            if (!match) {
								System.out.println("Skipped file: " + f.getName());
							}
                            return match;
                        })
                        .map(File::getName)
                        .sorted()
                        .collect(Collectors.toList());
            }
        } else {
            System.out.println("ERROR: Folder master not found at the location above.");
        }
        return new ArrayList<>();
    }
}