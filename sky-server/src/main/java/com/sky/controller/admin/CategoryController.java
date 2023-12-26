package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin/category")
@Api(tags = "分类相关接口")
@Slf4j
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping
    @ApiOperation(value = "新增分类")
    public Result addCategory(@RequestBody Category category){
        categoryService.addCategory(category);
        return Result.success();
    }

    @GetMapping("/page")
    @ApiOperation(value = "分类分页查询")
    public Result<PageResult> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO){
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    @PostMapping("/status/{status}")
    @ApiOperation(value = "启用禁用分类")
    public Result startOrStop(@PathVariable Integer status, Long id){
        categoryService.startOrStop(status,id);
        return Result.success();
    }

    @PutMapping
    @ApiOperation(value = "修改分类")
    public Result update(@RequestBody Category category){
        categoryService.update(category);
        return Result.success();
    }

    @DeleteMapping
    @ApiOperation(value = "根据id删除")
    public Result deleteById(Long id){
        categoryService.deleteById(id);
        return Result.success();
    }

    @GetMapping("/list")
    @ApiOperation(value = "查询所有分类")
    public Result<List<Category>> list(){
        List<Category> list = categoryService.list();
        return Result.success(list);
    }

}
