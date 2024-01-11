package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.entity.Orders;
import com.sky.entity.Setmeal;
import com.sky.entity.User;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */

        //查询总订单数
        Integer totalOrderCount = orderMapper.selectCount(new QueryWrapper<Orders>().between("order_time", begin, end));

        //有效订单数
        Integer validOrderCount = 0;

        //营业额
        List<Orders> ordersList = orderMapper.selectList(new QueryWrapper<Orders>()
                .between("order_time", begin, end)
                .eq("status", 5));
        Double turnover = 0.0;
        for (Orders orders: ordersList) {
            validOrderCount++;
            turnover = turnover + orders.getAmount().doubleValue();
        }

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0 && validOrderCount != 0){
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }

        //新增用户数
        Integer newUsers = userMapper.selectCount(new QueryWrapper<User>()
                .between("create_time", begin, end));

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }


    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOrderOverView() {

        LocalDateTime begin = LocalDateTime.now().with(LocalTime.MIN);
        //查询待接单的订单数量
        Integer toBeConfirmed = orderMapper.selectCount(new QueryWrapper<Orders>().eq("status", Orders.TO_BE_CONFIRMED).lt("order_time", begin));
        //查询待派送的订单数量
        Integer confirmed = orderMapper.selectCount(new QueryWrapper<Orders>().eq("status", Orders.CONFIRMED).lt("order_time", begin));
        //已完成
        Integer completedOrders = orderMapper.selectCount(new QueryWrapper<Orders>().eq("status", Orders.COMPLETED).lt("order_time", begin));
        //已取消
        Integer cancelledOrders = orderMapper.selectCount(new QueryWrapper<Orders>().eq("status", Orders.CANCELLED).lt("order_time", begin));
        //全部订单
        Integer allOrders = orderMapper.selectCount(new QueryWrapper<Orders>().lt("order_time", begin));

        return OrderOverViewVO.builder()
                .waitingOrders(toBeConfirmed)
                .deliveredOrders(confirmed)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .allOrders(allOrders)
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {
        //StatusConstant.ENABLE
        Integer sold = dishMapper.selectCount(new QueryWrapper<Dish>().eq("status", StatusConstant.ENABLE));

        // StatusConstant.DISABLE);
        Integer discontinued = dishMapper.selectCount(new QueryWrapper<Dish>().eq("status", StatusConstant.DISABLE));

        return DishOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        // StatusConstant.ENABLE
        Integer sold = setmealMapper.selectCount(new QueryWrapper<Setmeal>().eq("status", StatusConstant.ENABLE));

        //StatusConstant.DISABLE);
        Integer discontinued = setmealMapper.selectCount(new QueryWrapper<Setmeal>().eq("status", StatusConstant.DISABLE));

        return SetmealOverViewVO.builder()
                .sold(sold)
                .discontinued(discontinued)
                .build();
    }
}
