package com.example.group.entity;

import com.example.entity.BaseBean;
import lombok.Data;

import java.util.List;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:12
 * @Description TODO
 */
@Data
public class GroupMemberResBean<T> extends BaseBean {
    private Integer code;
    private String msg;
    private List<T> lists;

    @Override
    public Byte code() {
        return 12;
    }
}
