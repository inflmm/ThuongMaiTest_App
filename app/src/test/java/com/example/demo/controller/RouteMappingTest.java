package com.example.demo.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.demo.model.Category;
import com.example.demo.model.Collection;
import com.example.demo.service.BlogService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.CollectionService;
import com.example.demo.service.ProductService;

@ExtendWith(MockitoExtension.class)
class RouteMappingTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @Mock
    private BlogService blogService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CollectionService collectionService;

    @BeforeEach
    void setUp() {
        WebController webController = new WebController();
        ReflectionTestUtils.setField(webController, "productService", productService);
        ReflectionTestUtils.setField(webController, "blogService", blogService);
        ReflectionTestUtils.setField(webController, "categoryService", categoryService);

        CollectionController collectionController = new CollectionController();
        ReflectionTestUtils.setField(collectionController, "collectionService", collectionService);

        mockMvc = MockMvcBuilders.standaloneSetup(webController, collectionController).build();
    }

    @Test
    void categoryRouteShouldRenderFilterPage() throws Exception {
        Category category = new Category();
        category.setName("Electronics");
        category.setSlug("electronics");

        when(categoryService.getCategoryBySlug("electronics")).thenReturn(Optional.of(category));
        when(categoryService.buildCategoryPathSlugs(category)).thenReturn("electronics");

        mockMvc.perform(get("/categories/electronics"))
                .andExpect(status().isOk())
                .andExpect(view().name("filter-page"))
                .andExpect(model().attribute("currentCategoryName", "Electronics"))
                .andExpect(model().attribute("currentCategorySlug", "electronics"));
    }

    @Test
    void collectionRouteShouldRenderCollectionPage() throws Exception {
        Collection collection = new Collection();
        collection.setName("Summer Sale");
        collection.setSlug("summer-sale");

        when(collectionService.getCollectionBySlug("summer-sale")).thenReturn(collection);

        mockMvc.perform(get("/collection/summer-sale"))
                .andExpect(status().isOk())
                .andExpect(view().name("pages/collections"));
    }
}
