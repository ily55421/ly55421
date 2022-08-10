package com.example.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * @Author: linK
 * @Date: 2022/8/9 14:23
 * @Description TODO
 */
@Getter
@AllArgsConstructor
public enum BusinessOrderEnum {
    /**
     * 业务指令枚举集
     */
    LOGIN_REQUEST((byte)1), LOGIN_RESPONSE((byte)2), MESSAGE_REQUEST((byte)3), MESSAGE_RESPONSE((byte)4), MESSAGE_RECEIVE((byte)5);
    private Byte value;

//    BusinessOrderEnum(int i) {
//    }
//
//    void test() {
//        byte i =  1;
//        get(i);
//    }
//    public void get(byte b){
//        System.out.println(b);
//    }

    //    public static void main(String[] args) {
//        System.out.println(0b1
//        );
//    }
    public BusinessOrderEnum getValue(byte value) {

        return Arrays.stream(BusinessOrderEnum.values()).filter(m -> m.getValue() == value).findFirst().orElse(null);
    }




}
