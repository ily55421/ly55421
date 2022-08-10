package com.example.group.entity;

import com.example.entity.BaseBean;
import lombok.Data;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:02
 * @Description TODO
 */
@Data
public class GroupAddReqBean  extends BaseBean {
    private Integer userId;
    private Integer groupId;

    @Override
    public Byte code() {
        return 7;
    }
}
