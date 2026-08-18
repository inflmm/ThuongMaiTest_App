package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.CustomRoute;
import com.example.demo.repository.CustomRouteRepository;

@Service
public class CustomRouteService {

    @Autowired
    private CustomRouteRepository customRouteRepository;

    public Optional<CustomRoute> findVisibleBySlug(String slug) {
        return customRouteRepository.findBySlugAndVisibleTrueAndDeletedFalse(slug);
    }

    public List<CustomRoute> getAllForAdmin() {
        return customRouteRepository.findByDeletedFalse();
    }

    public Optional<CustomRoute> getById(Long id) {
        return customRouteRepository.findById(id);
    }

    public CustomRoute create(CustomRoute route) {
        return customRouteRepository.save(route);
    }

    public CustomRoute update(CustomRoute route) {
        return customRouteRepository.save(route);
    }

    public void delete(Long id) {
        customRouteRepository.findById(id).ifPresent(route -> {
            route.setDeleted(true);
            customRouteRepository.save(route);
        });
    }
}
