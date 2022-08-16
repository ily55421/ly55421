package com;

import com.link.codegen.processor.dto.GenDto;
import lombok.Data;

/**
 * @author lin 2022/8/17 0:16
 */
@Data
@GenDto(pkgName = "com.xzg.cd.rpc.demo.client.dto")
public class CodeValue {


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
