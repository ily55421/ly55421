package com.example.util;

import com.example.entity.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: linK
 * @Date: 2022/8/9 14:37
 * @Description TODO 为什么需要这么一个工具类呢？
 * 指令表示的是业务类型，不同的业务对应不同的实体，那么解码的时候，怎么知道反序列化成什么样的实体呢？
 * TODO 思路是获取到的指令，再根据指令找到对应的实体即可。
 */
public class MapUtils {
    /**
     * 1. 自定义指令
     */
    private static Byte codeLoginReq=1;
    private static Byte codeLoginRes=2;
    private static Byte codeMsgReq=3;
    private static Byte codeMsgRes=4;
    private static Byte codeMsgRec=5;

    /**
     * 2. 自定义一个Map，专门管理指令和实体的关系
     */
    private static Map<Byte, Class<? extends BaseBean>> map=new HashMap<Byte,Class<? extends BaseBean>>();
    //3. 初始化
    static {
        map.put(codeLoginReq, LoginReqBean.class);
        map.put(codeLoginRes, LoginResBean.class);
        map.put(codeMsgReq, MsgReqBean.class);
        map.put(codeMsgRes, MsgResBean.class);
        map.put(codeMsgRec, MsgRecBean.class);
    }

    //4. 根据指令获取对应的实体
    public static Class<? extends BaseBean> getBean(Byte code){
        try{
            return map.get(code);
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
}