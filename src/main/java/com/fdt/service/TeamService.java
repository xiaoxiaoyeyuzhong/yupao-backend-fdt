package com.fdt.service;

import com.fdt.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fdt.model.domain.User;
import com.fdt.model.request.TeamAddRequest;

import javax.servlet.http.HttpServletRequest;

/**
* @author 冯德田
* description 针对表【team(队伍)】的数据库操作Service
* createDate 2024-08-15 15:30:51
*/
public interface TeamService extends IService<Team> {
    /**
     * 创建队伍
     * @param team 队伍
     * @param request 请求
     * @return Long 队伍id
     */
    long addTeam(Team team, HttpServletRequest request);
}
