package com.fdt.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author tian
 * 用户包装类（脱敏）
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = -7234826075952356765L;

    /**
     * id
     */
    private long id;
    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户对象
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;


    /**
     * 电话号码
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态 0-正常
     */
    private Integer userStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;


    /**
     * 用户角色 0-普通用户 1-管理员
     */
    private Integer userRole;

    /**
     * 星球编号
     */
    private String planetCode;

    /**
     * 标签列表tags
     */
    private String tags;

}
