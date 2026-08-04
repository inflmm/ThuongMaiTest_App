package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.demo.dto.CategoryAdminDto;
import com.example.demo.dto.CategoryTreeNodeDto;
import com.example.demo.model.Category;
import com.example.demo.model.Product;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;

@DataJpaTest
@Import({CategoryService.class, ProductService.class})
class ProductCategoryServiceTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Test
    void shouldCreateNestedCategoriesAndExposeRootTree() {
        Category parent = categoryService.createCategory("Electronics", null);
        Category child = categoryService.createCategory("Phones", parent.getId());
        Category grandChild = categoryService.createCategory("Smartphones", child.getId());

        List<Category> roots = categoryService.getRootCategories();
        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).getName()).isEqualTo("Electronics");

        List<Category> children = categoryService.getChildren(parent.getId());
        assertThat(children).extracting(Category::getName).containsExactly("Phones");

        List<Category> grandchildren = categoryService.getChildren(child.getId());
        assertThat(grandchildren).extracting(Category::getName).containsExactly("Smartphones");

        Category stored = categoryRepository.findById(parent.getId()).orElseThrow();
        assertThat(stored.getChildren()).hasSize(1);
        assertThat(grandChild.getParent()).isEqualTo(child);
    }

    @Test
    void shouldAssignSequentialDisplayOrderToSiblings() {
        Category first = categoryService.createCategory("Electronics", null);
        Category second = categoryService.createCategory("Books", null);
        Category child = categoryService.createCategory("Phones", first.getId());

        assertThat(first.getDisplayOrder()).isEqualTo(1);
        assertThat(second.getDisplayOrder()).isEqualTo(2);
        assertThat(child.getDisplayOrder()).isEqualTo(1);
    }

    @Test
    void shouldExposeCategoryTreeAsDtoWithPlainChildStructure() {
        Category parent = categoryService.createCategory("Electronics", null);
        categoryService.createCategory("Phones", parent.getId());

        List<CategoryAdminDto> tree = categoryService.getCategoryTreeDto();
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("Electronics");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getName()).isEqualTo("Phones");
        assertThat(tree.get(0).getChildren().get(0).getParentId()).isEqualTo(parent.getId());
    }

    @Test
    void shouldExposeStorefrontCategoryTreeWithCountsAndSlugPaths() {
        Category parent = categoryService.createCategory("Electronics", null);
        categoryService.createCategory("Phones", parent.getId());

        List<CategoryTreeNodeDto> tree = categoryService.getCategoryTreeForStorefront();
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("Electronics");
        assertThat(tree.get(0).getSlugPath()).isEqualTo("/electronics/");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getSlugPath()).isEqualTo("/electronics/phones/");
    }

    @Test
    void shouldAdjustCategoryCountsWhenProductCategoryChanges() {
        Category firstCategory = categoryService.createCategory("Electronics", null);
        Category secondCategory = categoryService.createCategory("Books", null);

        Product product = new Product();
        product.setName("Phone");
        product.setSlug("phone");
        product.setCategory(firstCategory);
        product.setVisible(true);
        product.setAvailable(true);
        product.setDeleted(false);
        productService.createOrUpdateProduct(product);

        Category refreshedFirst = categoryRepository.findById(firstCategory.getId()).orElseThrow();
        Category refreshedSecond = categoryRepository.findById(secondCategory.getId()).orElseThrow();
        assertThat(refreshedFirst.getProductCount()).isEqualTo(1);
        assertThat(refreshedFirst.getAdminProductCount()).isEqualTo(1);
        assertThat(refreshedSecond.getProductCount()).isZero();
        assertThat(refreshedSecond.getAdminProductCount()).isZero();

        product.setCategory(secondCategory);
        productService.createOrUpdateProduct(product);

        Category updatedFirst = categoryRepository.findById(firstCategory.getId()).orElseThrow();
        Category updatedSecond = categoryRepository.findById(secondCategory.getId()).orElseThrow();
        assertThat(updatedFirst.getProductCount()).isZero();
        assertThat(updatedFirst.getAdminProductCount()).isZero();
        assertThat(updatedSecond.getProductCount()).isEqualTo(1);
        assertThat(updatedSecond.getAdminProductCount()).isEqualTo(1);
    }

    @Test
    void shouldNotDoubleCountWhenUpdatingProductWithStaleCategoryPaths() {
        Category category = categoryService.createCategory("Electronics", null);

        Product product = new Product();
        product.setName("Phone");
        product.setSlug("phone");
        product.setCategory(category);
        product.setVisible(true);
        product.setAvailable(true);
        product.setDeleted(false);
        productService.createOrUpdateProduct(product);

        Product persisted = productRepository.findBySlug("phone").orElseThrow();
        persisted.setCategoryPathIds(null);
        persisted.setCategoryPathSlugs(null);
        productRepository.save(persisted);

        productService.createOrUpdateProduct(persisted);

        Category refreshedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(refreshedCategory.getProductCount()).isEqualTo(1);
        assertThat(refreshedCategory.getAdminProductCount()).isEqualTo(1);
    }

    @Test
    void shouldCountHiddenProductsForAdminButNotForPublic() {
        Category category = categoryService.createCategory("Electronics", null);

        Product hiddenProduct = new Product();
        hiddenProduct.setName("Hidden Product");
        hiddenProduct.setSlug("hidden-product");
        hiddenProduct.setCategory(category);
        hiddenProduct.setVisible(false);
        hiddenProduct.setAvailable(true);
        hiddenProduct.setDeleted(false);
        productService.createOrUpdateProduct(hiddenProduct);

        Category refreshedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        assertThat(refreshedCategory.getProductCount()).isZero();
        assertThat(refreshedCategory.getAdminProductCount()).isEqualTo(1);
    }

    @Test
    void shouldReturnProductsForCategoryAndDescendants() {
        Category parent = categoryService.createCategory("Electronics", null);
        Category child = categoryService.createCategory("Phones", parent.getId());
        Category other = categoryService.createCategory("Books", null);

        Product childProduct = new Product();
        childProduct.setName("Phone");
        childProduct.setSlug("phone");
        childProduct.setCategory(child);
        childProduct.setVisible(true);
        childProduct.setAvailable(true);
        childProduct.setDeleted(false);
        productService.createOrUpdateProduct(childProduct);

        Product parentProduct = new Product();
        parentProduct.setName("Laptop");
        parentProduct.setSlug("laptop");
        parentProduct.setCategory(parent);
        parentProduct.setVisible(true);
        parentProduct.setAvailable(true);
        parentProduct.setDeleted(false);
        productService.createOrUpdateProduct(parentProduct);

        Product otherProduct = new Product();
        otherProduct.setName("Novel");
        otherProduct.setSlug("novel");
        otherProduct.setCategory(other);
        otherProduct.setVisible(true);
        otherProduct.setAvailable(true);
        otherProduct.setDeleted(false);
        productService.createOrUpdateProduct(otherProduct);

        List<Product> products = categoryService.getProductsForCategorySlug("phones");
        assertThat(products).extracting(Product::getName).containsExactly("Phone");
    }

    @Test
    void shouldRebuildProductPathsAndCountsWhenCategoryTreeIsReparented() {
        Category electronics = categoryService.createCategory("Electronics", null);
        Category books = categoryService.createCategory("Books", null);
        Category phones = categoryService.createCategory("Phones", electronics.getId());

        Product product = new Product();
        product.setName("Phone");
        product.setSlug("phone");
        product.setCategory(phones);
        product.setVisible(true);
        product.setAvailable(true);
        product.setDeleted(false);
        productService.createOrUpdateProduct(product);

        categoryService.updateCategory(phones.getId(), "Phones", books.getId(), null, null);

        Product refreshed = productService.getProductBySlug("phone").orElseThrow();
        assertThat(refreshed.getCategoryPathIds()).isEqualTo("/" + books.getId() + "/" + phones.getId() + "/");

        Category electronicsRefreshed = categoryRepository.findById(electronics.getId()).orElseThrow();
        Category booksRefreshed = categoryRepository.findById(books.getId()).orElseThrow();
        Category phonesRefreshed = categoryRepository.findById(phones.getId()).orElseThrow();

        assertThat(electronicsRefreshed.getProductCount()).isZero();
        assertThat(booksRefreshed.getProductCount()).isEqualTo(1);
        assertThat(phonesRefreshed.getProductCount()).isEqualTo(1);
    }

    @Test
    void shouldRejectCreatingAFourthLevelCategory() {
        Category level1 = categoryService.createCategory("Electronics", null);
        Category level2 = categoryService.createCategory("Phones", level1.getId());
        Category level3 = categoryService.createCategory("Smartphones", level2.getId());

        assertThatThrownBy(() -> categoryService.createCategory("Accessories", level3.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3 levels");
    }

    @Test
    void shouldRejectSettingCategoryAsItsOwnParent() {
        Category category = categoryService.createCategory("Electronics", null);

        assertThatThrownBy(() -> categoryService.updateCategory(category.getId(), "Electronics", category.getId(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parent");
    }

    @Test
    void shouldRejectChangingParentWhenCategoryHasChildren() {
        Category parent = categoryService.createCategory("Electronics", null);
        Category child = categoryService.createCategory("Phones", parent.getId());
        Category otherParent = categoryService.createCategory("Books", null);

        assertThatThrownBy(() -> categoryService.updateCategory(parent.getId(), "Electronics", otherParent.getId(), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("child");
        assertThat(child.getParent()).isEqualTo(parent);
    }
}
