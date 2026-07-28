package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.model.Collection;
import com.example.demo.service.CollectionService;

@Controller
@RequestMapping("/collections")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @GetMapping("/{slug}")
    public String getCollectionPage(@PathVariable String slug, Model model) {
        Collection collection = collectionService.getCollectionBySlug(slug);

        if (collection == null) {
            return "error/404";
        }

        model.addAttribute("collection", collection);
        model.addAttribute("products", collection.getProducts());
        model.addAttribute("pageTitle", collection.getName());

        return "pages/collections"; // File HTML bạn đang làm
    }
}