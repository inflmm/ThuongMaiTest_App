package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class CategoryTreeNodeDto {
    private Long id;
    private String name;
    private String slug;
    private String slugPath;
    private Integer productCount;
    private List<CategoryTreeNodeDto> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSlugPath() {
        return slugPath;
    }

    public void setSlugPath(String slugPath) {
        this.slugPath = slugPath;
    }

    public Integer getProductCount() {
        return productCount;
    }

    public void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }

    public List<CategoryTreeNodeDto> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryTreeNodeDto> children) {
        this.children = children;
    }
}
