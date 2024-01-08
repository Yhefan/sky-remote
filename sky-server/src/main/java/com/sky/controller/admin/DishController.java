package com.sky.controller.admin;



import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关借口")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping
    @ApiOperation("新增菜品")
    public Result addDish(@RequestBody DishDTO dishDTO){
        dishService.addDish(dishDTO);
        //清除redis缓存
        redisTemplate.delete("dish_"+dishDTO.getCategoryId());
        return Result.success();
    }

    @GetMapping ("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> pageQuery(DishPageQueryDTO dishPageQueryDTO){
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用菜品")
    public Result startOrStop(@PathVariable Integer status, Long id){
        dishService.startOrStop(status,id);
        //清除redis缓存
        cleanCache("dish_*");
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id){
        DishVO dishVO = dishService.getById(id);
        return Result.success(dishVO);
    }


    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO){
        log.info("dishDTO:{}",dishDTO);
        dishService.update(dishDTO);
        //清除redis缓存
        cleanCache("dish_*");
        return Result.success();
    }

    @DeleteMapping
    @ApiOperation("删除菜品")
    public Result delete(Long... ids){
        dishService.delete(ids);
        //全删
        cleanCache("dish_*");
        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId){
        List<Dish> list = dishService.getByCategoryId(categoryId);
        return Result.success(list);
    }



    public void cleanCache(String pattern){
        Set keys = redisTemplate.keys(pattern);
        redisTemplate.delete(keys);
    }

}
