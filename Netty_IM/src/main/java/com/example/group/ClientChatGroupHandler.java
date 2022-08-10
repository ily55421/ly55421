package com.example.group;

import com.example.entity.LoginReqBean;
import com.example.entity.LoginResBean;
import com.example.group.entity.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

import java.util.Scanner;

/**
 * @Author: linK
 * @Date: 2022/8/10 15:55
 * @Description TODO
 */
public class ClientChatGroupHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //在链接就绪时登录
        login(ctx.channel());
    }

    //主要是“接受服务端”的响应信息
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof LoginResBean) {
            LoginResBean res = (LoginResBean) msg;
            System.out.println("登录响应：" + res.getMsg());
            if (res.getStatus() == 0) {
                //登录成功

                //1.给通道绑定身份
                ctx.channel().attr(AttributeKey.valueOf("userid")).set(res.getUserid());

                //2.显示操作类型【请看下面】
                deal(ctx.channel());
            } else {
                //登录失败，继续登录
                login(ctx.channel());
            }
        } else if (msg instanceof GroupCreateResBean) {
            GroupCreateResBean res = (GroupCreateResBean) msg;
            System.out.println("创建响应群组：" + res.getMsg());

        } else if (msg instanceof GroupListResBean) {
            GroupListResBean res = (GroupListResBean) msg;
            System.out.println("查看群组列表：" + res.getLists());

        } else if (msg instanceof GroupAddResBean) {
            GroupAddResBean res = (GroupAddResBean) msg;
            System.out.println("加入群组响应：" + res.getMsg());

        } else if (msg instanceof GroupQuitResBean) {
            GroupQuitResBean res = (GroupQuitResBean) msg;
            System.out.println("退群群组响应：" + res.getMsg());

        } else if (msg instanceof GroupMemberResBean) {
            GroupMemberResBean res = (GroupMemberResBean) msg;
            if (res.getCode() == 1) {
                System.out.println("查看成员列表：" + res.getMsg());
            } else {
                System.out.println("查看成员列表：" + res.getLists());
            }

        } else if (msg instanceof GroupSendMsgResBean) {
            GroupSendMsgResBean res = (GroupSendMsgResBean) msg;
            System.out.println("群发消息响应：" + res.getMsg());

        } else if (msg instanceof GroupRecMsgBean) {
            GroupRecMsgBean res = (GroupRecMsgBean) msg;
            System.out.println("收到消息fromuserid=" +
                    res.getFromUserId() +
                    ",msg=" + res.getMsg());
        }
    }

    /**
     * 通过子线程循环向输出控制台输出操作类型的方法，以下方法目前都是空方法，下面将详细讲解。
     *
     * @param channel
     */
    private void deal(final Channel channel) {
        final Scanner scanner = new Scanner(System.in);
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    System.out.println("请选择类型：0创建群组，1查看群组，2加入群组，3退出群组，4查看群成员，5群发消息");
                    int type = scanner.nextInt();
                    switch (type) {
                        case 0:
                            createGroup(scanner, channel);
                            break;
                        case 1:
                            listGroup(scanner, channel);
                            break;
                        case 2:
                            addGroup(scanner, channel);
                            break;
                        case 3:
                            quitGroup(scanner, channel);
                            break;
                        case 4:
                            listMembers(scanner, channel);
                            break;
                        case 5:
                            sendMsgToGroup(scanner, channel);
                            break;
                        default:
                            System.out.println("输入的类型不存在!");
                    }
                }
            }
        }).start();
    }

    private void login(Channel ctx) {
        Scanner scanner = new Scanner(System.in);
        System.out.println(">>用户ID：");
        Integer userid = scanner.nextInt();
        System.out.println(">>用户名称：");
        String username = scanner.next();

        LoginReqBean bean = new LoginReqBean();
        bean.setUserid(userid);
        bean.setUsername(username);
        ctx.writeAndFlush(bean);
    }

    private void createGroup(Scanner scanner, Channel channel) {
        System.out.println("请输入群组ID");
        Integer groupId = scanner.nextInt();
        System.out.println("请输入群组名称");
        String groupName = scanner.next();

        GroupCreateReqBean bean = new GroupCreateReqBean();
        bean.setGroupId(groupId);
        bean.setGroupName(groupName);
        channel.writeAndFlush(bean);
    }

    private void listGroup(Scanner scanner, Channel channel) {
        GroupListReqBean bean = new GroupListReqBean();
        bean.setType("list");
        channel.writeAndFlush(bean);
    }

    private void listMembers(Scanner scanner, Channel channel) {
        System.out.println("请输入群组ID：");
        int groupId = scanner.nextInt();

        GroupMemberReqBean bean = new GroupMemberReqBean();
        bean.setGroupId(groupId);
        channel.writeAndFlush(bean);
    }

    private void addGroup(Scanner scanner, Channel channel) {
        System.out.println("请输入加入的群组ID");
        int groupId = scanner.nextInt();
        Integer userId = (Integer) channel.attr(AttributeKey.valueOf("userid")).get();

        GroupAddReqBean bean = new GroupAddReqBean();
        bean.setUserId(userId);
        bean.setGroupId(groupId);
        channel.writeAndFlush(bean);
    }

    private void quitGroup(Scanner scanner, Channel channel) {
        System.out.println("请输入退出的群组ID");
        int groupId = scanner.nextInt();
        Integer userId = (Integer) channel.attr(AttributeKey.valueOf("userid")).get();

        GroupQuitReqBean bean = new GroupQuitReqBean();
        bean.setUserId(userId);
        bean.setGroupId(groupId);
        channel.writeAndFlush(bean);
    }

    private void sendMsgToGroup(Scanner scanner, Channel channel) {
        System.out.println("请输入群组ID：");
        int groupId = scanner.nextInt();

        System.out.println("请输入发送消息内容：");
        String msg = scanner.next();

        Integer userId = (Integer) channel.attr(AttributeKey.valueOf("userid")).get();

        GroupSendMsgReqBean bean = new GroupSendMsgReqBean();
        bean.setFromUserId(userId);
        bean.setToGroupId(groupId);
        bean.setMsg(msg);
        channel.writeAndFlush(bean);
    }
}