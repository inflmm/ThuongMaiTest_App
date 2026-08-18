package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.CustomRoute;

public interface CustomRouteRepository extends JpaRepository<CustomRoute, Long> {
    Optional<CustomRoute> findBySlugAndVisibleTrueAndDeletedFalse(String slug);
    List<CustomRoute> findByDeletedFalse();
}
