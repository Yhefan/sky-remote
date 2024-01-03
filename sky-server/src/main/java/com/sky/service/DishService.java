package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

public interface DishService {
    void addDish(DishDTO dishDTO);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void startOrStop(Integer status, Long id);

    void update(DishDTO dishDTO);

    DishVO getById(Long id);

    void delete(Long[] ids);
}
