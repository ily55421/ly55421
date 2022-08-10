package com.example.group;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:23
 * @Description TODO 组和成员列表关系实体
 */
@Data
public  class Group implements Serializable {
    private String groupName;
    private List<GroupMember> members=new ArrayList<GroupMember>();
}