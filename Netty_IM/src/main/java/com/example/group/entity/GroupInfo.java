package com.example.group.entity;

import com.example.entity.BaseBean;
import lombok.Data;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:04
 * @Description TODO
 */
@Data
public class GroupInfo extends BaseBean {
    private Integer groupId;
    private String groupName;

    @Override
    public Byte code() {
        return null;
    }
}
