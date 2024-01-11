package com.sky.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private WorkspaceService workspaceService;
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate startDate, LocalDate endDate) {
        TurnoverReportVO tVO = new TurnoverReportVO();
        //创建一个List，用于存放日期
        List<LocalDate> list = getDatesList(startDate, endDate);
        //将list转换为字符串，以逗号分隔
        tVO.setDateList(StringUtils.join(list, ","));
        //创建一个List，用于存放营业额
        List<BigDecimal> turnoverList = new ArrayList<>();
        for (LocalDate date : list) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //selcet sum(total_price) from orders where order_time between beginTime and endTime and status = 5
            List<Orders> ordersList = orderMapper.selectList(new QueryWrapper<Orders>()
                    .between("order_time", beginTime, endTime)
                    .eq("status", 5));
            BigDecimal total_price = BigDecimal.ZERO;
            for (Orders orders: ordersList) {
                total_price = total_price.add(orders.getAmount());
            }
            turnoverList.add(total_price);
        }
        //将turnoverList转换为字符串，以逗号分隔
        tVO.setTurnoverList(StringUtils.join(turnoverList, ","));
        return tVO;
    }

    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        UserReportVO userReportVO = new UserReportVO();
        //创建一个List，用于存放日期
        List<LocalDate> list = getDatesList(begin, end);
        //将list转换为字符串，以逗号分隔
        userReportVO.setDateList(StringUtils.join(list, ","));
        //创建一个List，用于存放用户总量
        List<Integer> totalUserList = new ArrayList<>();
        //创建一个List，用于存放新增用户
        List<Integer> newUserList = new ArrayList<>();
        for (LocalDate date : list) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //select count(*) from user where create_time < beginTime
            Integer totalUser = userMapper.selectCount(new QueryWrapper<User>()
                    .lt("create_time", beginTime));
            totalUserList.add(totalUser);
            //select count(*) from user where create_time between beginTime and endTime
            Integer newUser = userMapper.selectCount(new QueryWrapper<User>()
                    .between("create_time", beginTime, endTime));
            newUserList.add(newUser);
        }
        //将totalUserList转换为字符串，以逗号分隔
        userReportVO.setTotalUserList(StringUtils.join(totalUserList, ","));
        //将newUserList转换为字符串，以逗号分隔
        userReportVO.setNewUserList(StringUtils.join(newUserList, ","));
        return userReportVO;
    }



    @Override
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        OrderReportVO orderReportVO = new OrderReportVO();
        //创建一个List，用于存放日期
        List<LocalDate> list = getDatesList(begin, end);
        //将list转换为字符串，以逗号分隔
        orderReportVO.setDateList(StringUtils.join(list, ","));
        //创建一个List，用于存放每日订单数
        List<Integer> orderCountList = new ArrayList<>();
        //创建一个List，用于存放每日有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();
        Integer allTotalOrderCount = 0;//订单总数
        Integer allVlidOrderCount = 0;//有效订单总数
        for (LocalDate date : list) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //select count(*) from orders where order_time between beginTime and endTime
            Integer orderCount = orderMapper.selectCount(new QueryWrapper<Orders>()
                    .between("order_time", beginTime, endTime));
            orderCountList.add(orderCount);
            //select count(*) from orders where order_time between beginTime and endTime and status = 5
            Integer validOrderCount = orderMapper.selectCount(new QueryWrapper<Orders>()
                    .between("order_time", beginTime, endTime)
                    .eq("status", 5));
            validOrderCountList.add(validOrderCount);
            allTotalOrderCount += orderCount;
            allVlidOrderCount += validOrderCount;
        }
        orderReportVO.setOrderCountList(StringUtils.join(orderCountList, ","));
        orderReportVO.setValidOrderCountList(StringUtils.join(validOrderCountList, ","));
        orderReportVO.setTotalOrderCount(allTotalOrderCount);
        orderReportVO.setValidOrderCount(allVlidOrderCount);
        orderReportVO.setOrderCompletionRate(allVlidOrderCount.doubleValue() / allTotalOrderCount.doubleValue());
        return orderReportVO;
    }

    @Override
    public SalesTop10ReportVO salesTop10(LocalDate begin, LocalDate end) {
        SalesTop10ReportVO top10ReportVO = new SalesTop10ReportVO();
        //创建一个List，用于存放商品名称
        List<String> nameList = new ArrayList<>();
        //创建一个List，用于存放销量
        List<Object> numberList = new ArrayList<>();
        //select name,sum(number) from order_detail where order_id in (select id from orders where order_time between beginTime and endTime and status = 5) group by name order by sum(number) desc limit 10
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        orderDetailMapper.selectMaps(new QueryWrapper<OrderDetail>()
                .select("name", "sum(number) as number")
                .inSql("order_id", "select id from orders where order_time between '" + beginTime + "' and '" + endTime + "' and status = 5")
                .groupBy("name")
                .orderByDesc("sum(number)")
                .last("limit 10"))
                .forEach(map -> {
                    nameList.add((String) map.get("name"));
                    numberList.add(map.get("number"));
                });
        top10ReportVO.setNameList(StringUtils.join(nameList, ","));
        top10ReportVO.setNumberList(StringUtils.join(numberList, ","));
        return top10ReportVO;
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //查询数据库，获取最近30天的数据
        LocalDate begin =  LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().minusDays(1);
        //查得概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(begin,LocalTime.MIN), LocalDateTime.of(end,LocalTime.MAX));
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("ExcelTemplate/运营数据报表模板.xlsx");
        try {
            //基于模板文件创建一个新Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //获取sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            //填充数据
            sheet.getRow(1).getCell(1).setCellValue("时间："+ begin + "至" + end);
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());
            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = begin.plusDays(i);
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }
            //通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);
            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<LocalDate> getDatesList(LocalDate begin, LocalDate end) {
        List<LocalDate> list = new ArrayList<>();
        list.add(begin);
        //循环。从begin到end
        while (begin.isBefore(end)) {
            begin = begin.plusDays(1);
            list.add(begin);
        }
        return list;
    }
}
