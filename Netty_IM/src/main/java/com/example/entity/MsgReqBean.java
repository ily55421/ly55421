package com.example.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: linK
 * @Date: 2022/8/9 14:20
 * @Description TODO
 */
@Data
public class MsgReqBean extends BaseBean implements Serializable {
    /**
     * 发送人ID
     */
    private Integer fromuserid;
    /**
     * 接受人ID
     */
    private Integer touserid;
    /**
     * 发送消息
     */
    private String msg;

    public Byte code() {
        //业务指令
        return BusinessOrderEnum.MESSAGE_REQUEST.getValue();
    }
}
