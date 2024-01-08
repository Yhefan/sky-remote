package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    /**
     * 提交订单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //处理异常：地址为空，购物车为空
        AddressBook book = addressBookMapper.selectById(ordersSubmitDTO.getAddressBookId());
        if (book == null) {
            //抛出异常
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        List<ShoppingCart> list = shoppingCartMapper.selectList(new QueryWrapper<ShoppingCart>().eq("user_id", BaseContext.getCurrentId()));
        if (list == null || list.size() == 0) {
            //抛出异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //1.向订单表中插入数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(book.getPhone());
        orders.setConsignee(book.getConsignee());
        orders.setUserId(BaseContext.getCurrentId());
        orderMapper.insert(orders);
        //2.向订单详情表中插入n条数据
        for (ShoppingCart shoppingCart : list) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailMapper.insert(orderDetail);
        }
        //3.删除购物车中的数据
        shoppingCartMapper.delete(new QueryWrapper<ShoppingCart>().eq("user_id", BaseContext.getCurrentId()));
        //4.返回VO结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
        return orderSubmitVO;
    }
    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception{
//        // 当前登录用户id
//        Long userId = BaseContext.getCurrentId();
//        User user = userMapper.selectById(userId);
//
//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }
//
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));

        OrderPaymentVO vo = new OrderPaymentVO();
        vo.setNonceStr("666");
        vo.setPaySign("hhh");
        vo.setPackageStr("prepay_id=wx");
        vo.setSignType("RSA");
        vo.setTimeStamp("1670380960");
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.selectOne(new QueryWrapper<Orders>().eq("number", ordersPaymentDTO.getOrderNumber()));

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.updateById(orders);

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.selectOne(new QueryWrapper<Orders>().eq("number", outTradeNo));

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.updateById(orders);
    }
    /**
     * 查询订单详情
     */
    @Override
    public OrderVO orderDetail(Long id) {
        // 根据订单id查询订单
        Orders orders = orderMapper.selectById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        List<OrderDetail> list = orderDetailMapper.selectList(new QueryWrapper<OrderDetail>().eq("order_id", id));
        orderVO.setOrderDetailList(list);
        return orderVO;
    }

    @Override
    public void cancel(Long id) {
        // 根据订单id查询订单
        Orders orders = orderMapper.selectById(id);
        // 修改订单状态为已取消
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(orders);
    }

    @Override
    public void repetition(Long id) {
        //再来一单就是把原订单中的商品复制到购物车中
        orderDetailMapper.selectList(new QueryWrapper<OrderDetail>().eq("order_id", id)).forEach(orderDetail -> {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail, shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCartMapper.insert(shoppingCart);
        });
    }

    @Override
    public PageResult historyOrder(int page, int pageSize, Integer status) {
        IPage iPage = new Page(page, pageSize);
        IPage iPage1 = orderMapper.selectPage(iPage, new QueryWrapper<Orders>().eq("user_id", BaseContext.getCurrentId()).eq(status!=null,"status", status));
        List<OrderVO> list = new ArrayList();
        // 查询出订单明细，并封装入OrderVO进行响应
        if (iPage1 != null && iPage1.getTotal() > 0) {
            //对iPage1进行遍历
            iPage1.getRecords().forEach(orders -> {
                //把orders强制转换为Orders类型
                Orders order = (Orders) orders;
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                List<OrderDetail> orderDetails = orderDetailMapper.selectList(new QueryWrapper<OrderDetail>().eq("order_id", order.getId()));
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            });
        }
        return new PageResult(iPage1.getTotal(), list);
    }


}
