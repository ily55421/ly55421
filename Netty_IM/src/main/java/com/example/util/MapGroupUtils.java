package com.example.util;

import com.example.entity.BaseBean;
import com.example.entity.LoginReqBean;
import com.example.entity.LoginResBean;
import com.example.group.entity.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: linK
 * @Date: 2022/8/10 17:07
 * @Description TODO
 */
public class MapGroupUtils {
    private static Map<Byte, Class<? extends BaseBean>> mapEntity=new HashMap<Byte,Class<? extends BaseBean>>();
    static {
        //登录的请求和响应实体
        mapEntity.put((byte)1, LoginReqBean.class);
        mapEntity.put((byte)2, LoginResBean.class);

        //创建群组的请求和响应实体
        mapEntity.put((byte)3, GroupCreateReqBean.class);
        mapEntity.put((byte)4, GroupCreateResBean.class);

        //查看群组的请求和响应实体
        mapEntity.put((byte)5, GroupListReqBean.class);
        mapEntity.put((byte)6, GroupListResBean.class);

        //加入群组的请求和响应实体
        mapEntity.put((byte)7, GroupAddReqBean.class);
        mapEntity.put((byte)8, GroupAddResBean.class);

        //退出群组的请求和响应实体
        mapEntity.put((byte)9,GroupQuitReqBean.class);
        mapEntity.put((byte)10,GroupQuitResBean.class);

        //查看成员列表的请求和响应实体
        mapEntity.put((byte)11,GroupMemberReqBean.class);
        mapEntity.put((byte)12,GroupMemberResBean.class);

        //发送响应的实体（发送消息、发送响应、接受消息）
        mapEntity.put((byte)13,GroupSendMsgReqBean.class);
        mapEntity.put((byte)14,GroupSendMsgResBean.class);
        mapEntity.put((byte)15,GroupRecMsgBean.class);
    }
    //4. 根据指令获取对应的实体
    public static Class<? extends BaseBean> getBean(Byte code){
        try{
            return mapEntity.get(code);
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
}
