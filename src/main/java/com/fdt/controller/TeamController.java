package com.fdt.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fdt.common.BaseResponse;
import com.fdt.common.ErrorCode;
import com.fdt.common.ResultUtils;
import com.fdt.exception.BusinessException;
import com.fdt.model.domain.Team;
import com.fdt.model.domain.User;
import com.fdt.model.dto.TeamQuery;
import com.fdt.model.request.TeamAddRequest;
import com.fdt.model.vo.TeamUserVO;
import com.fdt.service.TeamService;
import com.fdt.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 队伍接口
 * @author fdt
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request){
        if (teamAddRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest,team);
        long teamId = teamService.addTeam(team, request);
        return ResultUtils.success(teamId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(long id){
        if (id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.removeById(id);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"删除失败");
        }

        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody Team team){
        if (team == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.updateById(team);
        if (!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"修改失败");
        }

        return ResultUtils.success(true);
    }
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id){
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        return ResultUtils.success(team);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> listTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        List<TeamUserVO> teamList = teamService.listTeam(teamQuery,isAdmin);
        return ResultUtils.success(teamList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery){
        if (teamQuery == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Team team = new Team();
        BeanUtils.copyProperties(teamQuery,team);
        Page<Team> page = new Page<>(teamQuery.getPageNum(),teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }
}
