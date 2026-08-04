package com.example.demo.service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.ProductAdminDto;
import com.example.demo.dto.ProductDetailDto;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.model.ProductSpecification;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductImageRepository;
import com.example.demo.repository.ProductRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired(required = false)
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired(required = false)
    private ProductImageService productImageService;

    @Autowired
    private CategoryService categoryService;

    public List<Product> getAllActiveProducts() {
        return productRepository.findByDeletedFalseAndVisibleTrueOrderByUpdatedTimeDesc();
    }

    @Transactional(readOnly = true)
    public List<ProductAdminDto> getAdminProducts() {
        return productRepository.findAllByDeletedFalseOrderByUpdatedTimeDesc().stream()
                .map(this::toAdminDto)
                .toList();
    }

    public Optional<Product> getProductBySlug(String slug) {
        return productRepository.findBySlugAndDeletedFalse(slug);
    }

    @Transactional(readOnly = true)
    public Optional<ProductAdminDto> getProductById(Long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .map(this::toAdminDto);
    }

    @Transactional
    public Product saveProduct(Product product) {
        if (product.getSlug() == null || product.getSlug().isEmpty()) {
            product.setSlug(generateSlug(product.getName()));
        }

        if (product.getAvailable() == null) {
            product.setAvailable(true);
        }
        if (product.getVisible() == null) {
            product.setVisible(true);
        }
        if (product.getStockQuantity() == null) {
            product.setStockQuantity(0);
        }
        if (product.getSpecifications() != null) {
            product.getSpecifications().forEach(spec -> spec.setProduct(product));
        }

        Product existing = product.getId() != null ? productRepository.findById(product.getId()).orElse(null) : null;
        categoryService.populatePathData(product);
        Product saved = productRepository.saveAndFlush(product);
        categoryService.recalculateAllCounts();

        return saved;
    }

    @Transactional
    public Product createOrUpdateProduct(Product product) {
        if (product.getId() != null) {
            Product existing = productRepository.findById(product.getId()).orElseThrow(() -> new IllegalArgumentException("Product not found"));
            product.setDeleted(existing.getDeleted());
            product.setImages(existing.getImages());
        } else {
            product.setDeleted(false);
        }
        return saveProduct(product);
    }

    public void softDeleteProduct(String slug) {
        Optional<Product> productOpt = productRepository.findBySlug(slug);
        productOpt.ifPresent(product -> {
            product.setDeleted(true);
            productRepository.save(product);
            categoryService.updateCountsForPath(product.getCategoryPathIds(), -1, product.getVisible() != null && product.getVisible(), true);
        });
    }

    @Transactional
    public void softDeleteProduct(Long id) {
        productRepository.findById(id).ifPresent(product -> {
            product.setDeleted(true);
            productRepository.save(product);
            categoryService.updateCountsForPath(product.getCategoryPathIds(), -1, product.getVisible() != null && product.getVisible(), true);
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

    @Transactional(readOnly = true)
    public Optional<ProductDetailDto> getProductDetailBySlug(String slug) {
        return productRepository.findBySlugAndDeletedFalse(slug)
                .map(p -> {
                    // Đảm bảo ProductImage đã tồn tại từ dữ liệu ảnh hiện có trong storage trước khi trả về DTO.
                    // Input: product entity có image_folder_path.
                    // Output: các bản ghi ProductImage được sync vào DB nếu chưa có.
                    syncProductImages(p);

                    ProductDetailDto dto = getProductDetailDto(p);
                    // masterFiles giữ kiểu danh sách tên file như trước để code hiện tại vẫn hoạt động.
                    // images là danh sách entity ProductImage chuẩn để dùng cho admin/product flow mới.
                    dto.setMasterFiles(getMasterImages(p.getImage_folder_path()));
                    dto.setImages(getProductImages(p));
                    return dto;
                });
    }

    private ProductAdminDto toAdminDto(Product product) {
        ProductAdminDto dto = new ProductAdminDto();
        dto.setId(product.getId());
        dto.setSlug(product.getSlug());
        dto.setName(product.getName());
        dto.setSku(product.getSku());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setAvailable(product.getAvailable());
        dto.setVisible(product.getVisible());
        dto.setShortDescription(product.getShortDescription());
        dto.setImageUrl(product.getImageUrl());
        dto.setImage_folder_path(product.getImage_folder_path());
        if (product.getCategory() != null) {
            ProductAdminDto.CategoryRefDto categoryRef = new ProductAdminDto.CategoryRefDto();
            categoryRef.setId(product.getCategory().getId());
            categoryRef.setName(product.getCategory().getName());
            categoryRef.setSlug(product.getCategory().getSlug());
            categoryRef.setLevel(product.getCategory().getLevel());
            dto.setCategory(categoryRef);
        }
        return dto;
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

        if (supabaseStorageService == null) {
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

    private void syncProductImages(Product product) {
        if (product == null) {
            return;
        }

        // Input: product entity với image_folder_path đã được set.
        // Output: nếu chưa có ProductImage nào cho product này, tạo mới từ các file master hiện có trong storage.
        List<ProductImage> existingImages = productImageRepository.findByProductAndDeletedFalseOrderByDisplayOrderAsc(product);
        if (!existingImages.isEmpty()) {
            return;
        }

        List<String> masterImages = getMasterImages(product.getImage_folder_path());
        List<ProductImage> newImages = new ArrayList<>();
        for (int i = 0; i < masterImages.size(); i++) {
            String masterFileName = masterImages.get(i);
            String largeObjectPath = product.getImage_folder_path() + "/master/" + masterFileName;
            String mediumObjectPath = product.getImage_folder_path() + "/grande/" + masterFileName.replace("_master.webp", "_grande.webp");
            String compactObjectPath = product.getImage_folder_path() + "/compact/" + masterFileName.replace("_master.webp", "_compact.webp");

            String largeUrl = productImageService != null ? productImageService.resolvePublicUrl(largeObjectPath) : null;
            String mediumUrl = productImageService != null ? productImageService.resolvePublicUrl(mediumObjectPath) : null;
            String compactUrl = productImageService != null ? productImageService.resolvePublicUrl(compactObjectPath) : null;

            newImages.add(new ProductImage(product, largeObjectPath, largeUrl, i, "large"));
            newImages.add(new ProductImage(product, mediumObjectPath, mediumUrl, i, "medium"));
            newImages.add(new ProductImage(product, compactObjectPath, compactUrl, i, "compact"));
            newImages.add(new ProductImage(product, compactObjectPath, compactUrl, i, "thumbnail"));
        }

        if (!newImages.isEmpty()) {
            productImageRepository.saveAll(newImages);
        }
    }

    private List<ProductImage> getProductImages(Product product) {
        if (product == null) {
            return new ArrayList<>();
        }

        // Input: product entity.
        // Output: danh sách ProductImage đã được lưu trong DB, sắp xếp theo displayOrder.
        // Expected return: list rỗng nếu chưa có image nào được sync.
        List<ProductImage> images = productImageRepository.findByProductAndDeletedFalseOrderByDisplayOrderAsc(product);
        if (!images.isEmpty()) {
            return images;
        }

        return new ArrayList<>();
    }
}
