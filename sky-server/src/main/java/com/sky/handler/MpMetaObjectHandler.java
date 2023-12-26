package com.sky.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.sky.context.BaseContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MpMetaObjectHandler implements MetaObjectHandler {
    LocalDateTime now = LocalDateTime.now();
    @Override
    public void insertFill(MetaObject metaObject) {
        Long id = BaseContext.getCurrentId();
        this.setFieldValByName("createTime", now,metaObject);
        this.setFieldValByName("updateTime",now,metaObject);
        this.setFieldValByName("createUser",id,metaObject);
        this.setFieldValByName("updateUser",id,metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long id = BaseContext.getCurrentId();
        this.setFieldValByName("updateUser",id,metaObject);
        this.setFieldValByName("updateTime",now,metaObject);
    }
}
