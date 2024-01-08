package com.sky.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理超时订单
     */
    @Scheduled(cron = "0 * * * * ? ")//每分钟执行一次
    public void handleTimeoutOrder() {
        log.info("处理超时订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        orderMapper.selectList(new QueryWrapper<Orders>().eq("status", Orders.PENDING_PAYMENT).lt("order_time", time)).forEach(order -> {
            order.setStatus(Orders.CANCELLED);
            order.setCancelReason("超时未支付");
            order.setCancelTime(LocalDateTime.now());
            orderMapper.updateById(order);
        });
    }

    /**
     * 处理一直在派送中的订单
     */
    @Scheduled(cron = "0 0 1 * * ? ")//每天凌晨1点执行一次
    public void handleDeliveryOrder() {
        log.info("处理一直在派送中的订单");
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        orderMapper.selectList(new QueryWrapper<Orders>().eq("status", Orders.DELIVERY_IN_PROGRESS).lt("order_time", time)).forEach(order -> {
            order.setStatus(Orders.COMPLETED);
            orderMapper.updateById(order);
        });
    }

}
