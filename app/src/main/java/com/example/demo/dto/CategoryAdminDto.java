package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdminDto {
    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private Integer level;
    private Integer displayOrder;
    private String iconUrl;
    private Integer productCount;
    private Integer adminProductCount;
    private List<CategoryAdminDto> children = new ArrayList<>();

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

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Integer getProductCount() {
        return productCount;
    }

    public void setProductCount(Integer productCount) {
        this.productCount = productCount;
    }

    public Integer getAdminProductCount() {
        return adminProductCount;
    }

    public void setAdminProductCount(Integer adminProductCount) {
        this.adminProductCount = adminProductCount;
    }

    public List<CategoryAdminDto> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryAdminDto> children) {
        this.children = children;
    }
}
