package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query(value = "select * from categories where deleted = false and parent_id is null order by displayOrder, name", nativeQuery = true)
    List<Category> findRootCategoriesForTree();

    @Query(value = "select * from categories where deleted = false and parent_id = :parentId order by displayOrder, name", nativeQuery = true)
    List<Category> findChildrenByParentIdForTree(@Param("parentId") Long parentId);

    @Query(value = "select * from categories where deleted = false order by displayOrder, name", nativeQuery = true)
    List<Category> findAllActiveCategoriesForTree();

    Optional<Category> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlug(String slug);
}
