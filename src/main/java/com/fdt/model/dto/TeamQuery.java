package com.fdt.model.dto;

import com.fdt.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

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
     * id列表
     */
    private List<Long> idList;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 搜索关键词
     */
    private String searchText;

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

    //判断队伍查询封装类除了继承分页类的属性，其他属性是否为空
    public boolean isEmptyExceptPage(){
        return id == null
                && name == null
                && description == null
                && searchText == null
                && maxNum == null && userId == null
                && status == null;
    }


}
