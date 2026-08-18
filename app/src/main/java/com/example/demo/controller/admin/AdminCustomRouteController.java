package com.example.demo.controller.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.CustomRoute;
import com.example.demo.service.CustomRouteService;

// Reads shared between ADMIN and EMPLOYEE. Writes are ADMIN-only: a bad
// custom route affects site-wide URL structure, not just one piece of
// content — a meaningfully different risk tier than editing a blog post.
@RestController
@RequestMapping("/api/admin/custom-routes")
public class AdminCustomRouteController {

    private final CustomRouteService customRouteService;

    public AdminCustomRouteController(CustomRouteService customRouteService) {
        this.customRouteService = customRouteService;
    }

    // ---------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<CustomRoute>> listRoutes() {
        return ResponseEntity.ok(customRouteService.getAllForAdmin());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomRoute> getRoute(@PathVariable Long id) {
        return customRouteService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------------
    // Writes (ADMIN-only — see class comment)
    // ---------------------------------------------------------------

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CustomRoute> createRoute(@RequestBody CustomRoute route) {
        return ResponseEntity.ok(customRouteService.create(route));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CustomRoute> updateRoute(@PathVariable Long id, @RequestBody CustomRoute route) {
        return customRouteService.getById(id)
                .map(existing -> {
                    route.setId(id);
                    return ResponseEntity.ok(customRouteService.update(route));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        customRouteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
