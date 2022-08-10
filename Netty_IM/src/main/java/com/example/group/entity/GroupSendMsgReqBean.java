package com.example.group.entity;

import com.example.entity.BaseBean;
import lombok.Data;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:14
 * @Description TODO
 */
@Data
public class GroupSendMsgReqBean extends BaseBean {
    private Integer fromUserId;
    private Integer toGroupId;
    private String msg;

    @Override
    public Byte code() {
        return 13;
    }
}
