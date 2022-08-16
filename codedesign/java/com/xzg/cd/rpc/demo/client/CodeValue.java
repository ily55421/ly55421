package com.xzg.cd.rpc.demo.client;

import com.link.codegen.processor.dto.GenDto;
import lombok.Data;

/**
 * @author lin 2022/8/17 0:09
 */
@Data
@GenDto(pkgName = "com.xzg.cd.rpc.demo.client.dto")
public class CodeValue {
    public static void main(String[] args) {
        System.out.println("ssss");
    }

    /**
     * key 键
     */
    private String k;

    /**
     * value 值
     */
    private String v;

    /**
     * label 标签
     */
    private String l;

}
