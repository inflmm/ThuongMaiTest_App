package com.example.demo.service;

import java.util.List;

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

    // Lấy tất cả danh mục (để hiện lên Menu chẳng hạn)
    public List<Collection> getAllCollections() {
        return collectionRepository.findAll();
    }
}