package com.example.group.entity;

import com.example.entity.BaseBean;
import lombok.Data;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:05
 * @Description TODO
 */
@Data
public class GroupAddResBean extends BaseBean {
    private Integer code;
    private String msg;

    @Override
    public Byte code() {
        return 8;
    }
}
