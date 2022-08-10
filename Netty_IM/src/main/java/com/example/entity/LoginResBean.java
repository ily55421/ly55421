package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: linK
 * @Date: 2022/8/9 14:19
 * @Description TODO
 */
@Data
public class LoginResBean extends BaseBean implements Serializable {
    /**
     * 响应状态，0登录成功，1登录失败
     */
    private Integer status;
    /**
     * 响应信息
     */
    private String msg;
    /**
     * 用户ID
     */
    private Integer userid;

    public Byte code() {
        //业务指令
        return BusinessOrderEnum.LOGIN_RESPONSE.getValue();
    }
}
