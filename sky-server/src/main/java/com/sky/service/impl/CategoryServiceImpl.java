package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @AutoFill(value = OperationType.INSERT)

    @Override
    public void addCategory(Category category) {
        category.setStatus(StatusConstant.DISABLE);
//        category.setCreateUser(BaseContext.getCurrentId());
//        category.setUpdateUser(BaseContext.getCurrentId());
//        category.setCreateTime(LocalDateTime.now());
//        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.insert(category);
    }

    @Override
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        QueryWrapper<Category> qW = new QueryWrapper<>();
        qW.like(StringUtils.isNotBlank(categoryPageQueryDTO.getName()),"name",categoryPageQueryDTO.getName());
        if (categoryPageQueryDTO.getType()!=null){
            qW.eq("status",1).eq("type",categoryPageQueryDTO.getType());
        }
        IPage page = new Page(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        IPage iPage = categoryMapper.selectPage(page, qW);
        List records = iPage.getRecords();
        long total = iPage.getTotal();
        return new PageResult(total,records);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        //TODO
        Category category = new Category();
        category.setId(id);
        category.setStatus(status);
        categoryMapper.updateById(category);
    }

    @AutoFill(value = OperationType.UPDATE)
    @Override
    public void update(Category category) {
//        category.setUpdateUser(BaseContext.getCurrentId());
//        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    @Override
    public void deleteById(Long id) {
        QueryWrapper<Dish> qw = new QueryWrapper<>();
        QueryWrapper<Setmeal> qw1 = new QueryWrapper<>();
        qw.eq("category_id",id);
        qw1.eq("category_id",id);
        if (dishMapper.selectCount(qw) > 0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }
        if (setmealMapper.selectCount(qw1) > 0){
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public List<Category> list(Integer type) {
        QueryWrapper<Category> qw = new QueryWrapper<>();
        if (type == null){
            qw.eq("status",1);
        }else {qw.eq("status",1).eq("type",type);}
        List<Category> categories = categoryMapper.selectList(qw);
        return categories;
    }

}
