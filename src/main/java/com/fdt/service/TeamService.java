package com.fdt.service;

import com.fdt.model.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fdt.model.domain.User;
import com.fdt.model.dto.TeamQuery;
import com.fdt.model.request.TeamAddRequest;
import com.fdt.model.request.TeamJoinRequest;
import com.fdt.model.request.TeamQuitRequest;
import com.fdt.model.request.TeamUpdateRequest;
import com.fdt.model.vo.TeamUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    /**
     * 队伍列表查询
     * @param teamQuery 查询条件
     * isAdmin 是否为管理员
     * @return List<TeamUserVO> 用户队伍封装类列表
     */
    List<TeamUserVO> listTeam(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍
     * @param teamUpdateRequest 队伍更新请求体
     * @param loginUser 登录用户信息
     * @return boolean 更新结果
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 用户加入队伍
     * @param teamJoinRequest 加入队伍请求
     * @param loginUser 登录用户信息
     * @return boolean 加入结果
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 用户退出队伍
     * @param teamQuitRequest 退出队伍请求
     * @param loginUser 登录用户信息
     * @return boolean 退出结果
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 队长解散队伍
     * @param teamId 队伍id
     * @param loginUser 登录用户信息
     * @return boolean 解散结果
     */
    boolean deleteTeam(long teamId, User loginUser);
}
