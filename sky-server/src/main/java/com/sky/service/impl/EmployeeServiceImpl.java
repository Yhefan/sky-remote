package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.annotation.AutoFill;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        //Employee employee = employeeMapper.getByUsername(username);
        //Mybatis plus
        QueryWrapper<Employee> qw = new QueryWrapper<Employee>();
        qw.eq("username",username);
        Employee employee = employeeMapper.selectOne(qw);



        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        //  后期需要进行md5加密，然后再进行比对
        // password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }
    @AutoFill(value = OperationType.INSERT)
   public void add(Employee employee) {
        employee.setStatus(StatusConstant.ENABLE);
        employee.setPassword(PasswordConstant.DEFAULT_PASSWORD);
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//
//        employee.setCreateUser(BaseContext.getCurrentId());
//        employee.setUpdateUser(BaseContext.getCurrentId());

        employeeMapper.insert(employee);
        //employeeMapper.add(employee);

    }

    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
//        PageHelper.startPage(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize());
//        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
//        long total = page.getTotal();
//        List<Employee> records = page.getResult();

        QueryWrapper<Employee> qw = new QueryWrapper<Employee>();
        qw.like(StringUtils.isNotBlank(employeePageQueryDTO.getName()),"username",employeePageQueryDTO.getName());
        IPage page1 = new Page(employeePageQueryDTO.getPage(),employeePageQueryDTO.getPageSize());
        IPage iPage = employeeMapper.selectPage(page1, qw);
        long total = iPage.getTotal();
        List<Employee> records = iPage.getRecords();


        return new PageResult(total,records);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setStatus(status);

        employeeMapper.updateById(employee);
    }

    @Override
    public Employee findById(Long id) {
        Employee employee = employeeMapper.selectById(id);
        if (employee!=null)
          employee.setPassword("****");
        return employee;
    }

    @AutoFill(value = OperationType.UPDATE)
    @Override
    public void update(Employee employee) {
      //  employee.setId(employee.getId());
//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.updateById(employee);
    }

}
