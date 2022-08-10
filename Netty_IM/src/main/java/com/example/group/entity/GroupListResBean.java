package com.example.group.entity;

import com.example.entity.BaseBean;
import lombok.Data;

import java.util.List;

/**
 * @Author: linK
 * @Date: 2022/8/10 16:03
 * @Description TODO
 */
@Data
public class GroupListResBean<T>  extends BaseBean {
    private List<T> lists;

    @Override
    public Byte code() {
        return 6;
    }
}
