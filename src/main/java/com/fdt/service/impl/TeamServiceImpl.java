package com.fdt.service.impl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fdt.common.ErrorCode;
import com.fdt.exception.BusinessException;
import com.fdt.mapper.TeamMapper;
import com.fdt.model.domain.Team;
import com.fdt.model.domain.TeamUser;
import com.fdt.model.domain.User;
import com.fdt.model.dto.TeamQuery;
import com.fdt.model.enums.TeamStatusEnum;
import com.fdt.model.request.TeamAddRequest;
import com.fdt.model.vo.TeamUserVO;
import com.fdt.model.vo.UserVO;
import com.fdt.service.TeamService;
import com.fdt.service.TeamUserService;
import com.fdt.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author 冯德田
 * description 针对表【team(队伍)】的数据库操作Service实现
 * createDate 2024-08-15 15:30:51
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserService userService;

    @Resource
    private TeamUserService teamUserService;

    /**
     * 创建队伍
     *
     * @param team 队伍
     * @return long 队伍id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, HttpServletRequest request) {
        // 1.请求参数是否为空
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2.是否登录
        if (userService.getLoginUser(request) == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        long userId = userService.getLoginUser(request).getId();
        //3.校验信息
//            1.队伍人数>1且<=20
        //获取最大人数，如果为null，设置默认值0
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数不满足要求");
        }
//            2.队伍标题<=20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题不满足要求");
        }
//            3.队伍描述<=512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
//            4.队伍状态枚举值校验
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (teamStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
//            5.如果队伍状态是保密，校验密码<=32
        String password = team.getPassword();
        if (teamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isNotBlank(password) && password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码不符合要求");
            }
        }
//            6.超时时间>当前时间
        Date expirationDate = team.getExpireTime();
        if (new Date().after(expirationDate)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间不满足要求");
        }
//            7.用户最多创建5个队伍
        // todo 这样写有bug，如果用户创建队伍前还未到达上限，可能可以同时创建很多队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamCount = this.count(queryWrapper);
        if (hasTeamCount >= 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多创建5个队伍");
        }
        //加入了@Transactional，声明这个方法的事务范围是整个方法，如果方法执行失败，则会回滚
        //但如果方法不想整个声明为事务，则不要用@Transactional
//            8.插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);

        if (!result || team.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
//            9.插入用户-队伍关系到关系表
        TeamUser teamUser = new TeamUser();
        teamUser.setUserId(userId);
        teamUser.setTeamId(team.getId());
        teamUser.setJoinTime(new Date());
        result = teamUserService.save(teamUser);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        return team.getId();
    }

    /**
     * 队伍列表查询
     *
     * @param teamQuery 查询条件
     * @param isAdmin 是否为管理员
     * @return List<TeamUserVO> 用户队伍封装类列表
     *
     */
    @Override
    public List<TeamUserVO> listTeam(TeamQuery teamQuery, boolean isAdmin) {
        //不能直接查询所有的队伍信息，如果查询条件为空，返回空列表
        if (teamQuery.isEmptyExceptPage()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不允许一次性查询所有数据");
        }
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //根据id查询
        Long id = teamQuery.getId();
        if (id != null) {
            if (id <=0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍id不合法");
            }
            queryWrapper.eq("id", id);
        }
        //根据队伍名称查询
        String name = teamQuery.getName();
        if (StringUtils.isNotBlank(name)) {
            if (name.length() >20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍名称过长");
            }
            queryWrapper.like("name", name);
        }
        //根据队伍描述查询
        String description = teamQuery.getDescription();
        if (StringUtils.isNotBlank(description)) {
            if (description.length() > 512) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
            }
            queryWrapper.like("description", description);
        }
        //根据关键词查询（队伍名称和描述）
        String searchText = teamQuery.getSearchText();

        if (StringUtils.isNotBlank(searchText)) {
            if (searchText.length()>20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索关键词过长");
            }
            queryWrapper.and(qw -> qw
                    .like("name", searchText)
                    .or()
                    .like("description", searchText));
        }
        //根据队伍最大人数查询
        Integer maxNum = teamQuery.getMaxNum();
        if (maxNum != null) {
            if(maxNum <= 0 || maxNum > 20){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数条件不满足要求");
            }
            queryWrapper.eq("maxNum", maxNum);
        }

        //根据用户（队长）id查询
        Long userId = teamQuery.getUserId();
        if (userId != null) {
            if(userId <= 0){
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户id条件不满足要求");
            }
            queryWrapper.eq("userId", userId);
        }
        //根据队伍状态来查询
        Integer status = teamQuery.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        //如果没有传递队伍状态，默认为公开
        if (teamStatusEnum == null) {
            teamStatusEnum = TeamStatusEnum.PUBLIC;
        }
        //如果不是管理员，想查询的队伍状态又不是公开，则不允许查询
        if (!isAdmin && !teamStatusEnum.equals(TeamStatusEnum.PUBLIC)) {
            throw new BusinessException(ErrorCode.NO_AUTH, "无权限查询队伍");
        }
        queryWrapper.eq("status", teamStatusEnum.getValue());
        //不展示已过期的队伍(过期时间为空或过期时间比当前时间早)
        queryWrapper.and(qw -> qw
                .isNull("expireTime")
                .or()
                //大于当前时间
                .gt("expireTime", new Date()));
        List<Team> teamList = this.list(queryWrapper);
        if (teamList == null) {
            return new ArrayList<>();
        }
        //关联查询创建人的用户信息
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList){
            userId = team.getUserId();
            if(userId == null){
                continue;
            }
            User user = userService.getById(userId);
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            //脱敏用户信息
            if(user != null){
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user, userVO);
                teamUserVO.setCreatedUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;

    }
}




