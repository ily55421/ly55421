package com.example.group;

import io.netty.channel.Channel;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:22
 * @Description TODO 成员和连接通道的关系实体
 */

@Data
public class GroupMember implements Serializable {
    private Integer userid;
    private Channel channel;
}