package com.maiu.erp.modules.inventory.domain.repository;

import java.util.*;
import com.maiu.erp.modules.inventory.domain.model.Category;

public interface CategoryRepository {
    Category save(Category category);

    Optional<Category> findById(UUID id);

    List<Category> findAll();

    List<Category> findActiveCategories();

    List<Category> findByParentId(UUID parentId);
}
