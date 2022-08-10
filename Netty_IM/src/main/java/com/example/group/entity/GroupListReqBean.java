package com.example.group.entity;

import com.example.entity.BaseBean;
import lombok.Data;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:00
 * @Description TODO
 */
@Data
public class GroupListReqBean extends BaseBean {
    private String type;

    @Override
    public Byte code() {
        return 5;
    }
}
