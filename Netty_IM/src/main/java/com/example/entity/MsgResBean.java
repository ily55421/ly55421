package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: linK
 * @Date: 2022/8/9 14:21
 * @Description TODO
 */
@Data
public class MsgResBean extends BaseBean implements Serializable {
    /**
     * 响应状态，0发送成功，1发送失败
     */
    private Integer status;
    /**
     * 响应信息
     */
    private String msg;

    public Byte code() {
        //业务指令
        return BusinessOrderEnum.MESSAGE_RESPONSE.getValue();
    }
}