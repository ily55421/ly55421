package com.example.nettytest._06packtest;

import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.template.AbstractTemplate;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

/**
 * @author lin 2022/8/11 23:08
 */
public class UserTemplate extends AbstractTemplate<UserInfo> {
    /**
     *
     * @param packer  打包的对象
     * @param userInfo 序列化的对象
     * @param required 是否必要
     * @throws IOException
     */
    @Override
    public void write(Packer packer, UserInfo userInfo, boolean required) throws IOException {
        if (userInfo == null) {
            // 对象为空 且是必须的参数
            if (required) {
                throw new MessageTypeException("Attempted to write null");
            } else {
                packer.writeNil();
                return;
            }
        } else {
            packer.write(userInfo);
            return;
        }
    }

    @Override
    public UserInfo read(Unpacker unpacker, UserInfo userInfo, boolean required) throws IOException {
        //非必要  且为空
        if (!required && unpacker.trySkipNil()) {
            return null;
        } else {
            return unpacker.read(UserInfo.class);
        }

    }

    /**
     * 序列化模板对象
     */
    private static final UserTemplate INSTANCE = new UserTemplate();

    public static UserTemplate getInstance() {
        return INSTANCE;
    }
}
