package com.fdt.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用的删除请求体
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = 1583551821084616972L;

    private long id;
}
