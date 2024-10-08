package com.fdt.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author tian
 * 队伍和用户封装类（脱敏）
 */
@Data
public class TeamUserVO implements Serializable {


    private static final long serialVersionUID = 1499103002230465141L;

    /**
     * id
     */
    private Long id;
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
     * 用户id
     */
    private Long userId;
    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 队伍创建人用户信息
     */
    UserVO createdUser;

    /**
     * 当前用户是否加入队伍
     */
    private boolean hasJoin = false;

    /**
     * 已加入用户的数量
     */
    private Integer hasJoinNum;
}
