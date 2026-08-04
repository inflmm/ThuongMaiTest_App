package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Collection;
import com.example.demo.model.Product;
import com.example.demo.repository.CollectionRepository;
import com.example.demo.repository.ProductRepository;

import jakarta.transaction.Transactional;

@Service
public class CollectionService {
	@Autowired private CollectionRepository collectionRepository;
    @Autowired private ProductRepository productRepository;

    // 1. Lấy collection kèm danh sách sản phẩm (đã gán thủ công)
    public Collection getCollectionBySlug(String slug) {
        return collectionRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Collection not found"));
    }

    // 2. Hàm hỗ trợ Admin gán sản phẩm vào Collection thủ công
    @Transactional
    public void addProductToCollection(Long collectionId, Long productId) {
        Collection collection = collectionRepository.findById(collectionId).get();
        Product product = productRepository.findById(productId).get();

        if (!collection.getProducts().contains(product)) {
            collection.getProducts().add(product);
            collectionRepository.save(collection);
        }
    }

    public Optional<Collection> getCollectionById(Long id) {
        return collectionRepository.findById(id);
    }

    @Transactional
    public Collection createCollection(Collection collection) {
        if (collection.getSlug() == null || collection.getSlug().isBlank()) {
            collection.setSlug(collection.getName() == null ? "collection" : slugify(collection.getName()));
        }
        return collectionRepository.save(collection);
    }

    @Transactional
    public Collection updateCollection(Long id, Collection payload) {
        Collection existing = collectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));
        if (payload.getName() != null) {
            existing.setName(payload.getName());
        }
        if (payload.getDescription() != null) {
            existing.setDescription(payload.getDescription());
        }
        if (payload.getSlug() != null && !payload.getSlug().isBlank()) {
            existing.setSlug(payload.getSlug());
        } else if (existing.getSlug() == null || existing.getSlug().isBlank()) {
            existing.setSlug(slugify(existing.getName()));
        }
        return collectionRepository.save(existing);
    }

    @Transactional
    public void deleteCollection(Long id) {
        collectionRepository.findById(id).ifPresent(collection -> {
            collectionRepository.delete(collection);
        });
    }

    // Lấy tất cả danh mục (để hiện lên Menu chẳng hạn)
    public List<Collection> getAllCollections() {
        return collectionRepository.findAll();
    }

    private String slugify(String value) {
        if (value == null || value.isBlank()) {
            return "collection";
        }
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("^-+|-+$", "");
    }
}