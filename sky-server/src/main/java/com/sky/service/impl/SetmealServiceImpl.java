package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        QueryWrapper<Setmeal> qW = new QueryWrapper<>();
        qW.like(setmeal.getName()!=null,"name",setmeal.getName()).
                eq(setmeal.getStatus()!=null,"status",setmeal.getStatus()).
                eq(setmeal.getCategoryId()!=null,"category_id",setmeal.getCategoryId());
        List<Setmeal> list = setmealMapper.selectList(qW);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    @Override
    public void addSetmeal(SetmealDTO setmealDTO) {
        //1.新增套餐
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.insert(setmeal);
        //2.保持套餐和菜品的关系
        List<SetmealDish> dishList = setmealDTO.getSetmealDishes();
        if (dishList!= null && dishList.size() > 0){
            for (SetmealDish dish : dishList) {
                if (dish.getDishId() == null){
                    continue;
                }
                dish.setSetmealId(setmeal.getId());
                setmealDishMapper.insert(dish);
            }
        }
    }

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        QueryWrapper<Setmeal> qW = new QueryWrapper<>();
        qW.like(setmealPageQueryDTO.getName()!=null,"name",setmealPageQueryDTO.getName()).
                eq(setmealPageQueryDTO.getStatus()!=null,"status",setmealPageQueryDTO.getStatus()).
                eq(setmealPageQueryDTO.getCategoryId()!=null,"category_id",setmealPageQueryDTO.getCategoryId());
        IPage page = new Page(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        IPage iPage = setmealMapper.selectPage(page, qW);
        List records = iPage.getRecords();
        long total = iPage.getTotal();
        return new PageResult(total,records);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if(status == StatusConstant.ENABLE){
            //select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = ?
            List<Dish> dishList = dishMapper.selectList(new QueryWrapper<Dish>().inSql("id", "select dish_id from setmeal_dish where setmeal_id = " + id));
            if(dishList != null && dishList.size() > 0){
                dishList.forEach(dish -> {
                    if(StatusConstant.DISABLE == dish.getStatus()){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        Setmeal setmeal = new Setmeal();
        setmeal.setId(id);
        setmeal.setStatus(status);
        setmealMapper.updateById(setmeal);
    }

    @Override
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.selectById(id);
        List<SetmealDish> dishList = setmealDishMapper.selectList(new QueryWrapper<SetmealDish>().eq("setmeal_id", id));
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(dishList);
        return setmealVO;
    }

    @Override
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        List<SetmealDish> dishList = setmealDTO.getSetmealDishes();
        setmealDishMapper.delete(new QueryWrapper<SetmealDish>().eq("setmeal_id", setmealDTO.getId()));
        if (dishList!= null && dishList.size() > 0){
            for (SetmealDish dish : dishList) {
                if (dish.getDishId() == null){
                    continue;
                }
                dish.setSetmealId(setmealDTO.getId());
                setmealDishMapper.insert(dish);
            }
        }
        setmealMapper.updateById(setmeal);
    }

    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.selectById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
            setmealMapper.deleteById(id);
            setmealDishMapper.delete(new QueryWrapper<SetmealDish>().eq("setmeal_id", id));
        }
    }
}
