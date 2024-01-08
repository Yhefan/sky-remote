package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DishServiceImpl implements DishService{
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Override
    @Transactional
    public void addDish(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.insert(dish);
        //获取新增的菜品id
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors!= null && flavors.size() > 0){
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
                dishFlavorMapper.insert(flavor);
            }
        }
    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        QueryWrapper<Dish> qW = new QueryWrapper<>();
        qW.like(dishPageQueryDTO.getName()!=null,"name",dishPageQueryDTO.getName()).
                eq(dishPageQueryDTO.getStatus()!=null,"status",dishPageQueryDTO.getStatus()).
                eq(dishPageQueryDTO.getCategoryId()!=null,"category_id",dishPageQueryDTO.getCategoryId());
        IPage page = new Page(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        IPage iPage = dishMapper.selectPage(page, qW);
        List records = iPage.getRecords();
        long total = iPage.getTotal();
        return new PageResult(total,records);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish = new Dish();
        dish.setId(id);
        dish.setStatus(status);
        dishMapper.updateById(dish);
    }
    @Override
    public DishVO getById(Long id) {
        Dish dish = dishMapper.selectById(id);
        List<DishFlavor> flavorList = dishFlavorMapper.selectList(new QueryWrapper<DishFlavor>().eq("dish_id", id));
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(flavorList);
        return dishVO;
    }

    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            dishMapper.deleteById(id);
        }
    }

    @Override
    public void update(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        // List<DishFlavor> flavorList = dishFlavorMapper.selectList(new QueryWrapper<DishFlavor>().eq("dish_id", dishDTO.getId()));
        List<DishFlavor> flavorList = dishDTO.getFlavors();
        dishFlavorMapper.delete(new QueryWrapper<DishFlavor>().eq("dish_id", dishDTO.getId()));
        if (flavorList!= null && flavorList.size() > 0){
            for (DishFlavor flavor : flavorList) {
                if (flavor.getName() == ""){
                    continue;
                }
                flavor.setDishId(dishDTO.getId());
                dishFlavorMapper.insert(flavor);
            }
        }
        dishMapper.updateById(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        QueryWrapper<Dish> qW = new QueryWrapper<>();
        qW.like(dish.getName()!=null,"name",dish.getName()).
                eq(dish.getStatus()!=null,"status",dish.getStatus()).
                eq(dish.getCategoryId()!=null,"category_id",dish.getCategoryId());
        List<Dish> dishList = dishMapper.selectList(qW);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            //List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());
            List<DishFlavor> flavors = dishFlavorMapper.selectList(new QueryWrapper<DishFlavor>().eq("dish_id", d.getId()));

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> getByCategoryId(Long categoryId) {
        QueryWrapper<Dish> qW = new QueryWrapper<>();
        qW.eq("category_id",categoryId);
        List<Dish> dishList = dishMapper.selectList(qW);
        return dishList;
    }


}
