package com.fdt.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 队伍退出请求体
 * @author fdt
 */
@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = -1353687491435328846L;

    /**
     * 队伍id
     */
    private Long teamId;
}
