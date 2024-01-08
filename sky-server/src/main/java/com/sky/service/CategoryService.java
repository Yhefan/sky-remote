package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

import java.util.List;

public interface CategoryService {
    void addCategory(Category category);

    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    void startOrStop(Integer status, Long id);

    void update(Category category);

    void deleteById(Long id);

    List<Category> list(Integer type);
}
