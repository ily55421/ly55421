package com.example.group;

import com.example.entity.LoginReqBean;
import com.example.entity.LoginResBean;
import com.example.group.entity.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

import java.util.*;

/**
 * @Author: linK
 * @Date: 2022/8/10 15:56
 * @Description TODO
 */

public class ServerChatGroupHandler extends ChannelInboundHandlerAdapter {
    private static Map<Integer, Channel> map=new HashMap<Integer, Channel>();
    private static Map<Integer, Group> groups=new HashMap<Integer, Group>();



    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof LoginReqBean) {
            //登录
            login((LoginReqBean) msg, ctx.channel());

        } else if (msg instanceof GroupCreateReqBean) {
            //创建群组
            createGroup((GroupCreateReqBean) msg, ctx.channel());

        } else if (msg instanceof GroupListReqBean) {
            //查看群组列表
            listGroup((GroupListReqBean) msg, ctx.channel());

        } else if (msg instanceof GroupAddReqBean) {
            //加入群组
            addGroup((GroupAddReqBean) msg, ctx.channel());

        } else if (msg instanceof GroupQuitReqBean) {
            //退出群组
            quitGroup((GroupQuitReqBean) msg, ctx.channel());

        } else if (msg instanceof GroupMemberReqBean) {
            //查看成员列表
            listMember((GroupMemberReqBean) msg, ctx.channel());
        } else if (msg instanceof GroupSendMsgReqBean) {
            //消息发送
            sendMsg((GroupSendMsgReqBean) msg, ctx.channel());
        }
    }

    private void login(LoginReqBean bean, Channel channel){
        LoginResBean res=new LoginResBean();

        //从map里面根据用户ID获取连接通道
        Channel c=map.get(bean.getUserid());

        if(c==null){
            //通道为空，证明该用户没有在线

            //1.添加到map
            map.put(bean.getUserid(),channel);
            //2.给通道赋值
            channel.attr(AttributeKey.valueOf("userid")).set(bean.getUserid());
            //3.响应
            res.setStatus(0);
            res.setMsg("登录成功");
            res.setUserid(bean.getUserid());
            channel.writeAndFlush(res);
        }else{
            //通道不为空，证明该用户已经在线了

            res.setStatus(1);
            res.setMsg("该账户目前在线");
            channel.writeAndFlush(res);
        }
    }
    private void createGroup(GroupCreateReqBean bean, Channel channel) {
        //定义一个响应实体
        GroupCreateResBean res = new GroupCreateResBean();
        //查询groups是否已经存在
        Group group = groups.get(bean.getGroupId());
        //判断是否已经存在
        if (group == null) {
            //定义群组实体
            Group g = new Group();
            //定义一个集合，专门存储成员
            List<GroupMember> members = new ArrayList<GroupMember>();
            //属性赋值
            g.setGroupName(bean.getGroupName());
            g.setMembers(members);
            //添加到Map里面
            groups.put(bean.getGroupId(), g);

            //响应信息
            res.setCode(0);
            res.setMsg("创建群组成功");
        } else {
            res.setCode(1);
            res.setMsg("该群组已经存在!");
        }
        channel.writeAndFlush(res);
    }

    private void listGroup(GroupListReqBean bean, Channel channel) {
        if ("list".equals(bean.getType())) {
            //定义一个响应实体
            GroupListResBean res = new GroupListResBean();
            //定义一个集合
            List<GroupInfo> lists = new ArrayList<GroupInfo>();
            //变量groups Map集合
            for (Map.Entry<Integer, Group> entry : groups.entrySet()) {
                Integer mapKey = entry.getKey();
                Group mapValue = entry.getValue();
                GroupInfo gi = new GroupInfo();
                gi.setGroupId(mapKey);
                gi.setGroupName(mapValue.getGroupName());
                lists.add(gi);
            }
            //把集合添加到响应实体里面
            res.setLists(lists);
            //开始写到客户端
            channel.writeAndFlush(res);
        }
    }

    private void addGroup(GroupAddReqBean bean, Channel channel) {
        GroupAddResBean res = new GroupAddResBean();
        //1.根据“群组ID”获取对应的“组信息”
        Group group = groups.get(bean.getGroupId());
        //2.“群组”不存在
        if (group == null) {
            res.setCode(1);
            res.setMsg("groupId=" + bean.getGroupId() + ",不存在!");
            channel.writeAndFlush(res);
            return;
        }
        //3.“群组”存在，则获取其底下的“成员集合”
        List<GroupMember> members = group.getMembers();
        boolean flag = false;
        //4.遍历集合，判断“用户”是否已经存在了
        for (GroupMember gm : members) {
            // gm.getUserid()==bean.getUserId()
            if (Objects.equals(gm.getUserid(), bean.getUserId())) {
                flag = true;
                break;
            }
        }
        if (flag) {
            res.setCode(1);
            res.setMsg("已经在群组里面,无法再次加入!");
        } else {
            //1.用户信息
            GroupMember gm = new GroupMember();
            gm.setUserid(bean.getUserId());
            gm.setChannel(channel);

            //2.添加到集合里面
            members.add(gm);

            //3.给“群组”重新赋值
            group.setMembers(members);

            res.setCode(0);
            res.setMsg("加入群组成功");
        }
        channel.writeAndFlush(res);
    }


    private void quitGroup(GroupQuitReqBean bean, Channel channel) {
        GroupQuitResBean res = new GroupQuitResBean();

        //1.根据“群组ID”获取对应的“组信息”
        Group group = groups.get(bean.getGroupId());
        if (group == null) {
            //2.群组不存在
            res.setCode(1);
            res.setMsg("groupId=" + bean.getGroupId() + ",不存在!");
            channel.writeAndFlush(res);
            return;
        }
        //3.群组存在，则获取其底下“成员集合”
        List<GroupMember> members = group.getMembers();
        //4.遍历集合，找到“当前用户”在集合的序号
        int index = -1;
        for (int i = 0; i < members.size(); i++) {
            if (Objects.equals(members.get(i).getUserid(), bean.getUserId())) {
                index = i;
                break;
            }
        }
        //5.如果序号等于-1，则表示“当前用户”不存在集合里面
        if (index == -1) {
            res.setCode(1);
            res.setMsg("userid=" + bean.getUserId() + ",不存在该群组里面!");
            channel.writeAndFlush(res);
            return;
        }
        //6.从集合里面删除“当前用户”
        members.remove(index);
        //7.给“群组”的“成员列表”重新赋值
        group.setMembers(members);
        res.setCode(0);
        res.setMsg("退出群组成功");
        channel.writeAndFlush(res);
    }

    private void listMember(GroupMemberReqBean bean, Channel channel) {
        GroupMemberResBean res = new GroupMemberResBean();
        List<Integer> lists = new ArrayList<Integer>();
        //1.根据“群组ID”获取对应的“组信息”
        Group group = groups.get(bean.getGroupId());
        if (group == null) {
            //2.查询的群组不存在
            res.setCode(1);
            res.setMsg("groupId=" + bean.getGroupId() + ",不存在!");
            channel.writeAndFlush(res);
        } else {
            //3.群组存在，则变量其底层的成员
            for (Map.Entry<Integer, Group> entry : groups.entrySet()) {
                Group g = entry.getValue();
                List<GroupMember> members = g.getMembers();
                for (GroupMember gm : members) {
                    lists.add(gm.getUserid());
                }
            }

            res.setCode(0);
            res.setMsg("查询成功");
            res.setLists(lists);
            channel.writeAndFlush(res);
        }
    }

    private void sendMsg(GroupSendMsgReqBean bean, Channel channel) {
        GroupSendMsgResBean res = new GroupSendMsgResBean();

        //1.根据“群组ID”获取对应的“组信息”
        Group group = groups.get(bean.getToGroupId());

        //2.给“发送人”响应，通知其发送的消息是否成功
        if (group == null) {
            res.setCode(1);
            res.setMsg("groupId=" + bean.getToGroupId() + ",不存在!");
            channel.writeAndFlush(res);
            return;
        } else {
            res.setCode(0);
            res.setMsg("群发消息成功");
            channel.writeAndFlush(res);
        }
        //3.根据“组”下面的“成员”，变量并且逐个推送消息
        List<GroupMember> members = group.getMembers();
        for (GroupMember gm : members) {
            GroupRecMsgBean rec = new GroupRecMsgBean();
            rec.setFromUserId(bean.getFromUserId());
            rec.setMsg(bean.getMsg());
            gm.getChannel().writeAndFlush(rec);
        }
    }
}