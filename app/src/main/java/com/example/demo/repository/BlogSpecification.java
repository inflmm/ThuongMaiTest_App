package com.example.demo.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.example.demo.model.Blog;

import jakarta.persistence.criteria.Predicate; // Lưu ý dùng jakarta thay vì javax

public class BlogSpecification {
    public static Specification<Blog> filterBlogs(Long id, String title, LocalDate fromDate, LocalDate toDate, String contentPath) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (id != null) {
                predicates.add(cb.equal(root.get("id"), id));
            }
            if (title != null && !title.isEmpty()) {
                // Tìm kiếm theo tiêu đề hoặc slug
                String keyword = "%" + title.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), keyword),
                    cb.like(cb.lower(root.get("slug")), keyword)
                ));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("publishTime"), fromDate.atStartOfDay()));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("publishTime"), toDate.atTime(LocalTime.MAX)));
            }
            if (contentPath != null && !contentPath.isEmpty()) {
                predicates.add(cb.like(root.get("contentPath"), contentPath + "%"));
            }

            // Chỉ lấy bài chưa xóa
            predicates.add(cb.equal(root.get("deleted"), false));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}