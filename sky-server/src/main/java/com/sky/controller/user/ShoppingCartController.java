package com.sky.controller.user;


import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/user/shoppingCart")
@RestController
@Api(tags = "购物车相关接口")
@Slf4j
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    @PostMapping("/add")
    public Result addShoppingCart(@RequestBody ShoppingCartDTO shoppingCartDTO){
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        log.info("shoppingCartDTO:{}",shoppingCartDTO);
        return Result.success();
    }
    @GetMapping("/list")
    public Result<List<ShoppingCart>> showShoppingCart(){
        List<ShoppingCart> list = shoppingCartService.showShoppingCart();
        return Result.success(list);
    }
    @DeleteMapping("clean")
    public Result cleanShoppingCart(){
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }
    //删除购物车中的某个商品
    @PostMapping("sub")
    public Result subShoppingCart(@RequestBody ShoppingCartDTO shoppingCartDTO){
        shoppingCartService.subShoppingCart(shoppingCartDTO);
        return Result.success();
    }
}
