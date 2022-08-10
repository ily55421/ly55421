package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: linK
 * @Date: 2022/8/9 14:17
 * @Description TODO
 */
@Data
public abstract class BaseBean implements Serializable {
    /**
     * 固定值，标识的是一个协议类型，不同协议对应不同的值
     */
    private Integer tag = 1;

    /**
     * 业务指令抽象方法
     * @return
     */
    public abstract Byte code();
}