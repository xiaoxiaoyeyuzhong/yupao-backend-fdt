package com.fdt.common;

import lombok.Data;

import java.io.Serializable;


@Data
public class PageRequest implements Serializable {


    private static final long serialVersionUID = -5232361230575271903L;
    /**
     * 页面大小
     */
    protected int pageSize;

    /**
     * 当前是第几页
     */
    protected int pageNum;
}
