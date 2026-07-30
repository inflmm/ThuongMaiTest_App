package com.example.demo.service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
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

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    public List<Product> getAllActiveProducts() {
        return productRepository.findByDeletedFalse();
    }

    public Optional<Product> getProductBySlug(String slug) {
        return productRepository.findBySlugAndDeletedFalse(slug);
    }

    @Transactional
    public Product saveProduct(Product product) {
        if (product.getSlug() == null || product.getSlug().isEmpty()) {
            product.setSlug(generateSlug(product.getName()));
        }

        if (product.getSpecifications() != null) {
            product.getSpecifications().forEach(spec -> spec.setProduct(product));
        }

        return productRepository.save(product);
    }

    public void softDeleteProduct(String slug) {
        Optional<Product> productOpt = productRepository.findBySlug(slug);
        productOpt.ifPresent(product -> {
            product.setDeleted(true);
            productRepository.save(product);
        });
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
        return productRepository.findByIdInAndDeletedFalse(ids);
    }

    public Optional<ProductDetailDto> getProductDetailBySlug(String slug) {
        return productRepository.findBySlugAndDeletedFalse(slug)
                .map(p -> {
                    ProductDetailDto dto = getProductDetailDto(p);
                    dto.setMasterFiles(getMasterImages(p.getImage_folder_path()));
                    return dto;
                });
    }

    private ProductDetailDto getProductDetailDto(Product p) {
        ProductDetailDto dto = new ProductDetailDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setPrice(p.getPrice());
        dto.setImageUrl(p.getImageUrl());
        dto.setImage_folder_path(p.getImage_folder_path());
        dto.setLongDescription(p.getLongDescription());

        if (p.getSpecifications() != null) {
            dto.setSpecifications(p.getSpecifications().stream()
                    .collect(Collectors.toMap(
                            ProductSpecification::getSpecKey,
                            ProductSpecification::getSpecValue,
                            (existing, replacement) -> existing
                    )));
        }
        return dto;
    }

    public List<String> getMasterImages(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String prefix = folderPath.endsWith("/master") ? folderPath : folderPath + "/master";
            return supabaseStorageService.listObjects(SupabaseStorageService.StorageRoot.IMAGES, prefix, false).stream()
                    .filter(name -> name.toLowerCase().endsWith("_master.webp"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
