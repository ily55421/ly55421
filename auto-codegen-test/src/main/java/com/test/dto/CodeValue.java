package com.test.dto;

import com.link.codegen.processor.dto.GenDto;
import lombok.Data;

/**
 * @author lin 2022/8/17 0:09
 * 测试编译生成 dto类
 */
@Data
@GenDto(pkgName = "com.test.dto")
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
