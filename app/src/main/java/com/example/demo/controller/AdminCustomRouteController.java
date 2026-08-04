package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/api/admin/custom-routes")
public class AdminCustomRouteController {

    @Autowired
    private CustomRouteService customRouteService;

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

    @PostMapping
    public ResponseEntity<CustomRoute> createRoute(@RequestBody CustomRoute route) {
        return ResponseEntity.ok(customRouteService.create(route));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomRoute> updateRoute(@PathVariable Long id, @RequestBody CustomRoute route) {
        return customRouteService.getById(id)
                .map(existing -> {
                    route.setId(id);
                    return ResponseEntity.ok(customRouteService.update(route));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        customRouteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
