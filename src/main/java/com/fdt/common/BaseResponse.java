package com.fdt.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 * @param <T>
 * @author fdt
 */
@Data
public class BaseResponse<T> implements Serializable {
    private int code;

    //使用泛型,提高类的可重用性
    private T data;

    private String message;

    private String description;

    public BaseResponse(int code, T data, String message, String description) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.description = description;
    }

    public BaseResponse(int code, T data) {
        //直接调用已存在的构造函数
        this(code, data,"","");
    }

    public BaseResponse(int code, T data, String message) {
        //直接调用已存在的构造函数
        this(code, data, message,"");
    }

    public BaseResponse(ErrorCode errorCode){
        this(errorCode.code(),null,errorCode.message(),errorCode.description());
    }
}
