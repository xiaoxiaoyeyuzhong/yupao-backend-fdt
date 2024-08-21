package com.fdt.model.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 加入队伍请求体
 * author fdt
 */
@Data
public class TeamJoinRequest implements Serializable {
    private static final long serialVersionUID = 4477963317311940884L;
    /**
     * id
     */
    private Long teamId;


    /**
     * 密码
     */
    private String password;
}
