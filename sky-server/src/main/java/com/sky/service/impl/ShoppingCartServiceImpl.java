package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    @Override
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        //添加进购物车时，先查数据库中是否有该商品，如果有，就更新数量，如果没有，就添加
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.selectList(new QueryWrapper<ShoppingCart>()
                .eq(shoppingCart.getUserId()!=null, "user_id", shoppingCart.getUserId())
                .eq(shoppingCart.getDishId()!=null, "dish_id", shoppingCart.getDishId())
                .eq(shoppingCart.getSetmealId()!=null, "setmeal_id", shoppingCart.getSetmealId())
                .eq(StringUtils.isNotBlank(shoppingCart.getDishFlavor()), "dish_flavor", shoppingCart.getDishFlavor()));
        //如果购物车中有该商品，就更新数量
        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateById(cart);
        }else {
            //如果购物车中没有该商品，就添加
            //判断是套餐还是菜品
            if (shoppingCart.getDishId() != null) {
                Dish dish = dishMapper.selectById(shoppingCart.getDishId());
                shoppingCart.setName(dish.getName());
                shoppingCart.setAmount(dish.getPrice());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setNumber(1);
            } else {
                Setmeal setmeal = setmealMapper.selectById(shoppingCart.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setAmount(setmeal.getPrice());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setNumber(1);
            }
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> list = shoppingCartMapper.selectList(new QueryWrapper<ShoppingCart>()
                .eq("user_id", userId));
        return list;
    }

    @Override
    public void cleanShoppingCart() {
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.delete(new QueryWrapper<ShoppingCart>()
                .eq("user_id", userId));
    }

    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        //判断是套餐还是菜品,如果该的商品数量大于1则减1，否则删除该商品
        if (shoppingCart.getDishId() != null) {
            ShoppingCart one = shoppingCartMapper.selectOne(new QueryWrapper<ShoppingCart>()
                    .eq("user_id", userId)
                    .eq("dish_id", shoppingCart.getDishId())
                    .eq("dish_flavor", shoppingCart.getDishFlavor()));
            if (one.getNumber() > 1) {
                one.setNumber(one.getNumber() - 1);
                shoppingCartMapper.updateById(one);
            } else {
                shoppingCartMapper.deleteById(one.getId());
            }
        } else {
            ShoppingCart one = shoppingCartMapper.selectOne(new QueryWrapper<ShoppingCart>()
                    .eq("user_id", userId)
                    .eq("setmeal_id", shoppingCart.getSetmealId()));
            if (one.getNumber() > 1) {
                one.setNumber(one.getNumber() - 1);
                shoppingCartMapper.updateById(one);
            } else {
                shoppingCartMapper.deleteById(one.getId());
            }
        }

    }


}
