package com.example.group.entity;

import com.example.entity.BaseBean;
import lombok.Data;

/**
 * @Author: linK
 * @Date: 2022/8/10 15:57
 * @Description TODO
 */
@Data
public class GroupCreateReqBean extends BaseBean {
    private Integer groupId;
    private String groupName;

    @Override
    public Byte code() {
        return 3;
    }
}
