package com.fdt.model.dto;

import com.fdt.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 队伍查询封装类
 * @author fdt
 */
@EqualsAndHashCode(callSuper =true)
@Data
public class TeamQuery extends PageRequest {

    private static final long serialVersionUID = 5743584918494085909L;
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
     * 创建人id
     */
    private Long userId;

    /**
     * 0-公开，1-私有 2-加密
     */
    private Integer status;


}
