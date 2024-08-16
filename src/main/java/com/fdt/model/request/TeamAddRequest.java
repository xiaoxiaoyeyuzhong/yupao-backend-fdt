package com.fdt.model.request;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * @author fdt
 * 队伍添加请求体
 */
@Data
public class TeamAddRequest implements Serializable {


    private static final long serialVersionUID = 8427499365716557418L;
    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 0-公开，1-私有 2-加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;

}
