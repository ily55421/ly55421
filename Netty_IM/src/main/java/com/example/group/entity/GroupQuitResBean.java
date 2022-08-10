package com.example.group.entity;

import com.example.entity.BaseBean;
import lombok.Data;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:07
 * @Description TODO
 */
@Data
public class GroupQuitResBean extends BaseBean {
    private Integer code;
    private String msg;

    @Override
    public Byte code() {
        return 10;
    }
}
