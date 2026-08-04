package com.example.demo.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.CustomRoute;
import com.example.demo.service.CustomRouteService;

@Controller
@RequestMapping("/custom-routes")
public class CustomRouteController {

    @Autowired
    private CustomRouteService customRouteService;

    @GetMapping("/{slug}")
    public String resolveRoute(@PathVariable("slug") String slug) {
        Optional<CustomRoute> route = customRouteService.findVisibleBySlug(slug);
        if (route.isEmpty()) {
            return "redirect:/homepage";
        }

        String targetPath = route.get().getTargetPath();
        if (targetPath == null || targetPath.isBlank()) {
            return "redirect:/homepage";
        }

        // Keep absolute URLs as-is, otherwise normalize to an absolute app path.
        if (targetPath.startsWith("http://") || targetPath.startsWith("https://")) {
            return "redirect:" + targetPath;
        }

        return "redirect:" + (targetPath.startsWith("/") ? targetPath : "/" + targetPath);
    }
}
