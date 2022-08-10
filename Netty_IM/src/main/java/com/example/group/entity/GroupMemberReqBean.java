package com.example.group.entity;

import com.example.entity.BaseBean;
import lombok.Data;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:13
 * @Description TODO
 */
@Data
public class GroupMemberReqBean extends BaseBean {
    private Integer groupId;
    private Integer UserId;

    @Override
    public Byte code() {
        return 11;
    }
}
