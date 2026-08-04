package com.example.demo.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CategoryAdminDto;
import com.example.demo.dto.CategoryTreeNodeDto;
import com.example.demo.model.Category;
import com.example.demo.model.Product;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> getRootCategories() {
        return categoryRepository.findRootCategoriesForTree();
    }

    @Transactional(readOnly = true)
    public List<Category> getChildren(Long parentId) {
        if (parentId == null) {
            return List.of();
        }
        return categoryRepository.findChildrenByParentIdForTree(parentId);
    }

    @Transactional(readOnly = true)
    public List<CategoryAdminDto> getCategoryTreeDto() {
        List<Category> allCategories = categoryRepository.findAllActiveCategoriesForTree();
        Map<Long, CategoryAdminDto> dtoById = new HashMap<>();
        List<CategoryAdminDto> roots = new ArrayList<>();

        for (Category category : allCategories) {
            CategoryAdminDto dto = toAdminDto(category);
            dtoById.put(category.getId(), dto);
        }

        for (Category category : allCategories) {
            CategoryAdminDto dto = dtoById.get(category.getId());
            if (dto == null) {
                continue;
            }

            if (category.getParent() != null && category.getParent().getId() != null) {
                CategoryAdminDto parentDto = dtoById.get(category.getParent().getId());
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                } else {
                    roots.add(dto);
                }
            } else {
                roots.add(dto);
            }
        }

        roots.sort(Comparator.comparing(CategoryAdminDto::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(CategoryAdminDto::getName, String.CASE_INSENSITIVE_ORDER));
        return roots;
    }

    public CategoryAdminDto toAdminDto(Category category) {
        CategoryAdminDto dto = new CategoryAdminDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setParentId(category.getParent() != null ? category.getParent().getId() : null);
        dto.setLevel(category.getLevel());
        dto.setDisplayOrder(category.getDisplayOrder());
        dto.setIconUrl(category.getIconUrl());
        dto.setProductCount(category.getProductCount() != null ? category.getProductCount() : 0);
        dto.setAdminProductCount(category.getAdminProductCount() != null ? category.getAdminProductCount() : 0);
        dto.setChildren(new ArrayList<>());
        return dto;
    }

    public CategoryTreeNodeDto toStorefrontDto(Category category) {
        CategoryTreeNodeDto dto = new CategoryTreeNodeDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setSlug(category.getSlug());
        dto.setSlugPath(buildSlugPath(category));
        dto.setProductCount(category.getProductCount() != null ? category.getProductCount() : 0);
        dto.setChildren(new ArrayList<>());
        return dto;
    }

    private String buildSlugPath(Category category) {
        if (category == null) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        Category current = category;
        while (current != null) {
            if (current.getSlug() != null && !current.getSlug().isBlank()) {
                parts.add(current.getSlug());
            }
            current = current.getParent();
        }

        java.util.Collections.reverse(parts);
        return parts.isEmpty() ? null : "/" + String.join("/", parts) + "/";
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeNodeDto> getCategoryTreeForStorefront() {
        List<Category> allCategories = categoryRepository.findAllActiveCategoriesForTree();
        Map<Long, CategoryTreeNodeDto> dtoById = new HashMap<>();
        List<CategoryTreeNodeDto> roots = new ArrayList<>();

        for (Category category : allCategories) {
            CategoryTreeNodeDto dto = toStorefrontDto(category);
            dtoById.put(category.getId(), dto);
        }

        for (Category category : allCategories) {
            CategoryTreeNodeDto dto = dtoById.get(category.getId());
            if (dto == null) {
                continue;
            }

            if (category.getParent() != null && category.getParent().getId() != null) {
                CategoryTreeNodeDto parentDto = dtoById.get(category.getParent().getId());
                if (parentDto != null) {
                    parentDto.getChildren().add(dto);
                } else {
                    roots.add(dto);
                }
            } else {
                roots.add(dto);
            }
        }

        roots.sort(Comparator.comparing(CategoryTreeNodeDto::getName, String.CASE_INSENSITIVE_ORDER));
        return roots;
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsForCategorySlug(String slug) {
        return getProductsForCategory(getCategoryBySlug(slug).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsForCategory(Category category) {
        if (category == null) {
            return List.of();
        }

        final String categoryPathSlugs = category.getSlugPath() != null && !category.getSlugPath().isBlank()
                ? category.getSlugPath()
                : buildCategoryPathSlugs(category);

        return productRepository.findByDeletedFalseAndVisibleTrueOrderByUpdatedTimeDesc().stream()
                .filter(product -> isProductInCategoryPath(product, categoryPathSlugs))
                .toList();
    }

    private boolean isProductInCategoryPath(Product product, String categoryPathSlugs) {
        if (product == null || product.getCategoryPathSlugs() == null || categoryPathSlugs == null) {
            return false;
        }

        String normalizedProductPath = normalizeCategoryPath(product.getCategoryPathSlugs());
        String normalizedCategoryPath = normalizeCategoryPath(categoryPathSlugs);
        return normalizedProductPath.equals(normalizedCategoryPath)
                || normalizedProductPath.startsWith(normalizedCategoryPath + "/");
    }

    private String normalizeCategoryPath(String path) {
        if (path == null) {
            return "";
        }
        return path.replaceAll("^/+|/+$", "");
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeExistingCategoryCounts() {
        recalculateAllCounts();
    }

    @Transactional
    public void recalculateAllCounts() {
        List<Category> categories = categoryRepository.findAllActiveCategoriesForTree();
        for (Category category : categories) {
            category.setProductCount(0);
            category.setAdminProductCount(0);
        }
        categoryRepository.saveAll(categories);

        List<Product> allProducts = productRepository.findByDeletedFalseOrderByUpdatedTimeDesc();
        for (Product product : allProducts) {
            populatePathData(product);
            productRepository.saveAndFlush(product);
        }

        for (Product product : allProducts) {
            boolean publicVisible = product.getVisible() != null && product.getVisible();
            boolean adminActive = product.getDeleted() == null || !product.getDeleted();
            String categoryPathIds = product.getCategoryPathIds();
            if (categoryPathIds == null || categoryPathIds.isBlank()) {
                continue;
            }
            updateCountsForPath(categoryPathIds, 1, publicVisible, adminActive);
        }
    }

    @Transactional
    public void updateCountsForPath(String categoryPathIds, int delta) {
        updateCountsForPath(categoryPathIds, delta, true, true);
    }

    @Transactional
    public void updateCountsForPath(String categoryPathIds, int delta, boolean visibleForPublic, boolean activeForAdmin) {
        if (categoryPathIds == null || categoryPathIds.isBlank()) {
            return;
        }

        String[] parts = categoryPathIds.split("/");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            try {
                Long categoryId = Long.parseLong(part);
                Category category = categoryRepository.findById(categoryId).orElse(null);
                if (category != null) {
                    if (visibleForPublic) {
                        Integer current = category.getProductCount() == null ? 0 : category.getProductCount();
                        category.setProductCount(Math.max(0, current + delta));
                    }
                    if (activeForAdmin) {
                        Integer currentAdmin = category.getAdminProductCount() == null ? 0 : category.getAdminProductCount();
                        category.setAdminProductCount(Math.max(0, currentAdmin + delta));
                    }
                    categoryRepository.save(category);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public String buildCategoryPathIds(Category category) {
        if (category == null) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        Category current = category;
        while (current != null) {
            if (current.getId() != null) {
                parts.add(String.valueOf(current.getId()));
            }
            current = current.getParent();
        }

        java.util.Collections.reverse(parts);
        return parts.isEmpty() ? null : "/" + String.join("/", parts) + "/";
    }

    public String buildCategoryPathSlugs(Category category) {
        if (category == null) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        Category current = category;
        while (current != null) {
            if (current.getSlug() != null && !current.getSlug().isBlank()) {
                parts.add(current.getSlug());
            }
            current = current.getParent();
        }

        java.util.Collections.reverse(parts);
        return parts.isEmpty() ? null : "/" + String.join("/", parts) + "/";
    }

    public void populatePathData(Product product) {
        if (product == null) {
            return;
        }

        Category category = product.getCategory();
        if (category != null && category.getId() != null) {
            category = categoryRepository.findById(category.getId()).orElse(category);
            product.setCategory(category);
        }

        product.setCategoryPathIds(buildCategoryPathIds(category));
        product.setCategoryPathSlugs(buildCategoryPathSlugs(category));
    }

    public Optional<Category> getCategoryBySlug(String slug) {
        return categoryRepository.findBySlugAndDeletedFalse(slug);
    }

    @Transactional
    public Category createCategory(String name, Long parentId) {
        return createCategory(name, parentId, 0, null);
    }

    @Transactional
    public Category createCategory(String name, Long parentId, Integer displayOrder, String iconUrl) {
        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId).orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
        }

        int computedLevel = parent == null ? 1 : parent.getLevel() + 1;
        if (computedLevel > 3) {
            throw new IllegalArgumentException("Category depth cannot exceed 3 levels");
        }

        Category category = new Category();
        category.setName(name);
        category.setSlug(generateSlug(name));
        category.setParent(parent);
        category.setLevel(computedLevel);
        category.setDisplayOrder(resolveDisplayOrder(parent, displayOrder));
        category.setIconUrl(iconUrl);
        category.setDeleted(false);
        return categoryRepository.saveAndFlush(category);
    }

    @Transactional
    public Category updateCategory(Long categoryId, String name, Long parentId, Integer displayOrder, String iconUrl) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Category not found"));
        Category parent = null;
        if (parentId != null) {
            parent = categoryRepository.findById(parentId).orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
        }

        validateParentAssignment(category, parent, parentId);

        boolean parentChanged = isParentChanged(category, parentId);
        if (parentChanged && !category.getChildren().isEmpty()) {
            throw new IllegalArgumentException("Cannot change the parent of a category that already has child categories");
        }

        int computedLevel = parent == null ? 1 : parent.getLevel() + 1;
        if (computedLevel > 3) {
            throw new IllegalArgumentException("Category depth cannot exceed 3 levels");
        }

        String previousSlug = category.getSlug();
        Category previousParent = category.getParent();
        category.setName(name);
        category.setSlug(generateSlug(name));
        category.setParent(parent);
        category.setLevel(computedLevel);
        category.setDisplayOrder(resolveDisplayOrder(parent, displayOrder));
        category.setIconUrl(iconUrl);
        categoryRepository.saveAndFlush(category);

        if (parentChanged || !Objects.equals(previousSlug, category.getSlug())) {
            recalculateAllCounts();
        }
        return category;
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Category not found"));
        if (hasDescendants(category)) {
            throw new IllegalArgumentException("Cannot delete a category that has child categories");
        }
        if (productRepository.existsByCategoryIdAndDeletedFalse(categoryId)) {
            throw new IllegalArgumentException("Cannot delete a category that has products");
        }
        category.setDeleted(true);
        categoryRepository.save(category);
        recalculateAllCounts();
    }

    private int resolveDisplayOrder(Category parent, Integer requestedOrder) {
        if (requestedOrder != null && requestedOrder > 0) {
            return requestedOrder;
        }

        if (parent == null) {
            List<Category> roots = categoryRepository.findRootCategoriesForTree();
            return roots.stream()
                    .mapToInt(category -> category.getDisplayOrder() != null ? category.getDisplayOrder() : 0)
                    .max()
                    .orElse(0) + 1;
        }

        List<Category> children = categoryRepository.findChildrenByParentIdForTree(parent.getId());
        return children.stream()
                .mapToInt(category -> category.getDisplayOrder() != null ? category.getDisplayOrder() : 0)
                .max()
                .orElse(0) + 1;
    }

    private void validateParentAssignment(Category category, Category parent, Long requestedParentId) {
        if (requestedParentId != null && category.getId() != null && requestedParentId.equals(category.getId())) {
            throw new IllegalArgumentException("A category cannot select itself as a parent");
        }

        if (parent == null) {
            return;
        }

        Category current = parent;
        while (current != null) {
            if (category.getId() != null && current.getId() != null && current.getId().equals(category.getId())) {
                throw new IllegalArgumentException("A category cannot be assigned to one of its own descendants");
            }
            current = current.getParent();
        }
    }

    private boolean isParentChanged(Category category, Long requestedParentId) {
        if (category.getParent() == null) {
            return requestedParentId != null;
        }
        return requestedParentId == null || !requestedParentId.equals(category.getParent().getId());
    }

    private boolean hasDescendants(Category category) {
        if (category == null || category.getChildren() == null || category.getChildren().isEmpty()) {
            return false;
        }
        for (Category child : category.getChildren()) {
            if (child != null) {
                return true;
            }
        }
        return false;
    }

    public String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.isBlank()) {
            slug = "category";
        }
        return slug;
    }
}
