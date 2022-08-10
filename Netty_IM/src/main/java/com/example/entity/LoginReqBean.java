package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: linK
 * @Date: 2022/8/9 14:18
 * @Description TODO
 */
@Data
public class LoginReqBean extends BaseBean implements Serializable {
    /**
     * 用户ID
     */
    private Integer userid;
    /**
     * 用户名称
     */
    private String username;
    public Byte code() {
        //业务指令
        return BusinessOrderEnum.LOGIN_REQUEST.getValue();
    }
}
