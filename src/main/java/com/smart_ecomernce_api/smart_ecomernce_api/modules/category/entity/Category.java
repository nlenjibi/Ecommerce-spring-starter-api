package com.smart_ecomernce_api.smart_ecomernce_api.modules.category.entity;


import com.smart_ecomernce_api.smart_ecomernce_api.common.base.BaseEntity;
import com.smart_ecomernce_api.smart_ecomernce_api.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_slug", columnList = "slug"),
        @Index(name = "idx_category_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, exclude = {"parent", "children", "product"})
public class Category extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    // Self-referencing relationship for hierarchy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = false)
    @OrderBy("displayOrder ASC, name ASC")
    @Builder.Default
    private List<Category> children = new ArrayList<>();


    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<Product> products = new HashSet<>();

    // Helper methods
    public void addChild(Category child) {
        children.add(child);
        child.setParent(this);
    }

    public void removeChild(Category child) {
        children.remove(child);
        child.setParent(null);
    }

    public boolean isRootCategory() {
        return parent == null;
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public int getLevel() {
        int level = 0;
        Category current = this.parent;
        while (current != null) {
            level++;
            current = current.parent;
        }
        return level;
    }

    @PreRemove
    private void preRemove() {
        // When deleting a category, reassign its children to its parent
        if (!children.isEmpty()) {
            for (Category child : children) {
                child.setParent(this.parent);
            }
        }
    }
}
