package com.fdt.service.impl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fdt.common.ErrorCode;
import com.fdt.exception.BusinessException;
import com.fdt.mapper.TeamMapper;
import com.fdt.model.domain.Team;
import com.fdt.model.domain.TeamUser;
import com.fdt.model.domain.User;
import com.fdt.model.dto.TeamQuery;
import com.fdt.model.enums.TeamStatusEnum;
import com.fdt.model.request.TeamJoinRequest;
import com.fdt.model.request.TeamQuitRequest;
import com.fdt.model.request.TeamUpdateRequest;
import com.fdt.model.vo.TeamUserVO;
import com.fdt.model.vo.UserVO;
import com.fdt.service.TeamService;
import com.fdt.service.TeamUserService;
import com.fdt.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Resource
    private RedissonClient redissonClient;

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
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isNotBlank(password) && password.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍密码不符合要求");
            }
        }
//            6.超时时间>当前时间
        Date expirationDate = team.getExpireTime();
        if (expirationDate!=null && new Date().after(expirationDate)) {
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
     * @param teamQuery 队伍查询请求体
     * @param isAdmin 是否为管理员
     * @return List<TeamUserVO> 用户队伍封装类列表
     *
     */
    @Override
    public List<TeamUserVO> listTeam(TeamQuery teamQuery, boolean isAdmin) {
//        //不能直接查询所有的队伍信息，如果查询条件为空，返回空列表
//        if (teamQuery.isEmptyExceptPage()) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不允许一次性查询所有数据");
//        }
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //根据id查询
        Long id = teamQuery.getId();
        if (id != null) {
            if (id <=0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍id不合法");
            }
            queryWrapper.eq("id", id);
        }
        //根据id列表进行查询
        List<Long> idList = teamQuery.getIdList();
        if (CollectionUtils.isNotEmpty(idList)) {
            queryWrapper.in("id", idList);
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

        if(teamStatusEnum != null){
            //如果不是管理员，想查询的队伍状态又不是公开，则不允许查询
            if (!isAdmin && teamStatusEnum.equals(TeamStatusEnum.PRIVATE)) {
                throw new BusinessException(ErrorCode.NO_AUTH, "无权限查询队伍");
            }
            queryWrapper.eq("status", teamStatusEnum.getValue());
        }

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

    /**
     * 更新队伍
     * @param TeamUpdateRequest 队伍更新请求体
     * @param loginUser 登录用户信息
     * @return boolean 更新结果
     */
    @Override
    public boolean updateTeam(TeamUpdateRequest TeamUpdateRequest, User loginUser) {
        //判断参数是否为空
        if(TeamUpdateRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取队伍id
        Long id = TeamUpdateRequest.getId();
        if(id == null || id <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"要修改的队伍id不合法");
        }
        //获取队伍信息
        Team oldTeam = this.getById(id);
        if(oldTeam == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"要修改的队伍不存在");
        }
        //只有管理员或队长用户可以修改队伍信息
        if(!oldTeam.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH,"没有权限修改该队伍信息");
        }
        //如果要把队伍状态改为加密，需要提供密码
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(TeamUpdateRequest.getStatus());
        if(TeamStatusEnum.SECRET.equals(teamStatusEnum)){
            if(StringUtils.isBlank(TeamUpdateRequest.getPassword())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"加密队伍必须设置密码");
            }
        }
        Team updateTeam = new Team();
        // todo 比较旧的和要更新的队伍信息，如果有变化则更新

        BeanUtils.copyProperties(TeamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

    /**
     * 用户加入队伍
     * @param teamJoinRequest 加入队伍请求
     * @param loginUser 登录用户信息
     * @return boolean 加入结果
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        //判断请求体是否为空
        if(teamJoinRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //获取队伍id
        Long teamId = teamJoinRequest.getTeamId();
        if(teamId == null || teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"要加入的队伍id不合法");
        }
        Team team = this.getById(teamId);
        //判断队伍是否存在
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"要加入的队伍不存在");
        }
        //不能加入已过期的队伍
        if(team.getExpireTime() != null && team.getExpireTime().before(new Date())){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该队伍已过期");
        }
        //不能加入私有的队伍
        TeamStatusEnum teamStatusEnum=TeamStatusEnum.getEnumByValue(team.getStatus());
        if(TeamStatusEnum.PRIVATE.equals(teamStatusEnum)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"该队伍是私有的，不能加入");
        }
        //如果要加入加密队伍，需提供密码
        if(TeamStatusEnum.SECRET.equals(teamStatusEnum)){
            String password = teamJoinRequest.getPassword();
            if(StringUtils.isBlank(password) || !password.equals(team.getPassword())){
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"请提供正确的队伍密码");
            }
        }
        long userId = loginUser.getId();
        //用户最多加入5个队伍
        //分布式锁
        RLock lock = redissonClient.getLock("yupao:team:join");
        try{
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    QueryWrapper<TeamUser> teamUserQueryWrapper = new QueryWrapper<>();
                    teamUserQueryWrapper.eq("userId", userId);
                    long hadJoinedTeamNum = teamUserService.count(teamUserQueryWrapper);
                    if (hadJoinedTeamNum >= 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户最多加入5个队伍");
                    }
                    //用户不能重复加入同一个队伍
                    teamUserQueryWrapper = new QueryWrapper<>();
                    teamUserQueryWrapper.eq("teamId", teamId);
                    teamUserQueryWrapper.eq("userId", userId);
                    long userJoinTeamNum = teamUserService.count(teamUserQueryWrapper);
                    if (userJoinTeamNum > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已经加入该队伍");
                    }
                    //用户加入的队伍必须是未满的
                    teamUserQueryWrapper = new QueryWrapper<>();
                    teamUserQueryWrapper.eq("teamId", teamId);
                    long teamHaveUserNum = teamUserService.count(teamUserQueryWrapper);
                    if (team.getMaxNum() <= teamHaveUserNum) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    //新增队伍和用户关联表数据。
                    TeamUser teamUser = new TeamUser();
                    teamUser.setTeamId(teamId);
                    teamUser.setUserId(userId);
                    teamUser.setJoinTime(new Date());
                    return teamUserService.save(teamUser);
                }
            }
        } catch (InterruptedException e) {
            log.error("UserJoin error",e);
            return false;
        }finally {
            //锁的释放不能放try，要保证手动释放一定执行，防止看门狗机制一直续期
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }
    }

    /**
     * 用户退出队伍
     * @param teamQuitRequest 退出队伍请求
     * @param loginUser 登录用户信息
     * @return boolean 退出结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        //校验参数是否为空
        if(teamQuitRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //校验队伍id是否合法
        Long teamId = teamQuitRequest.getTeamId();
        if(teamId == null || teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"要退出的队伍id不合法");
        }
        //校验队伍是否存在
        Team team = this.getById(teamId);
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"要退出的队伍不存在");
        }
        //校验登录用户是否加入队伍
        long userId = loginUser.getId();
        TeamUser queryTeamUser = new TeamUser();
        queryTeamUser.setTeamId(teamId);
        queryTeamUser.setUserId(userId);
        QueryWrapper<TeamUser> teamUserQueryWrapper = new QueryWrapper<>(queryTeamUser);
        long count = teamUserService.count(teamUserQueryWrapper);
        if(count <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户未加入该队伍");
        }
        //查询队伍有多少人
        queryTeamUser = new TeamUser();
        teamUserQueryWrapper = new QueryWrapper<>(queryTeamUser);
        queryTeamUser.setTeamId(teamId);
        long teamHaveUserNum = teamUserService.count(teamUserQueryWrapper);
        //如果队伍只剩一个人，则直接删除队伍
        if(teamHaveUserNum == 1){
            //删除队伍
            this.removeById(teamId);
        }else{//队伍里有多个人
            //判断登录用户是不是队长
            if(team.getUserId()==userId){
                //把队伍转移给第二早加入队伍的用户（队长是第一个加入的）
                teamUserQueryWrapper = new QueryWrapper<>();
                teamUserQueryWrapper.eq("teamId",teamId);
                //查出两条数据就够了，id更早的是队长的，id晚一点是下一任队长的id
                teamUserQueryWrapper.last("order by id asc limit 2");
                List<TeamUser> teamUserList = teamUserService.list(teamUserQueryWrapper);
                if(CollectionUtils.isEmpty(teamUserList) || teamUserList.size() <= 1){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                //找到第二早加入队伍用户（下一任队长）的关系
                TeamUser nextCaptain = teamUserList.get(1);
                //获得下一任队长的id
                Long nextTeamLeaderId = nextCaptain.getUserId();
                //更新为当前队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setUserId(nextTeamLeaderId);
                boolean result = this.updateById(updateTeam);
                if(!result){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR,"更新队伍队长失败");
                }
            }
        }
        //删除队伍和用户关联表数据（无论队伍剩余多少人，登录用户是不是队长，都要删除关联表数据）
        teamUserQueryWrapper = new QueryWrapper<>();
        teamUserQueryWrapper.eq("teamId",teamId);
        teamUserQueryWrapper.eq("userId",userId);
        return teamUserService.remove(teamUserQueryWrapper);
    }

    /**
     * 队长解散队伍
     * @param teamId 队伍id
     * @param loginUser 登录用户信息
     * @return boolean 解散结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long teamId, User loginUser) {
        //校验id是否合法
        if(teamId <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"队伍id不合法");
        }
        //校验队伍是否存在
        Team team = this.getById(teamId);
        if(team == null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"要解散的队伍不存在");
        }
        //校验登录用户是否是要解散队伍的队长
        if(team.getUserId() != loginUser.getId()){
            throw new BusinessException(ErrorCode.NO_AUTH,"用户不是队长，没有解散队伍的权限");
        }
        //移除所有加入队伍的关联关系
        QueryWrapper<TeamUser> teamUserQueryWrapper = new QueryWrapper<>();
        teamUserQueryWrapper.eq("teamId",teamId);
        boolean result = teamUserService.remove(teamUserQueryWrapper);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"移除所有加入队伍的关联关系失败");
        }
        //删除队伍
        return this.removeById(teamId);
    }

    /**
     * 标记当前登录用户已经加入哪些队伍，并查询队伍中的用户数
     * @param originTeamList 原始队伍列表
     * @param request 请求
     * @return List<TeamUserVO> 处理后的用户列表
     */
    @Override
    public List<TeamUserVO> flagUserJoinedTeams(List<TeamUserVO> originTeamList, HttpServletRequest request) {
        //获取原始队伍列表的队伍id集合
        List<Long> teamIdList = originTeamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        //使用try，catch，避免getLoginUser出现异常，让不想登录的用户无法访问
        try {
            //通过team_user表，根据登录用户的userId和teamId查询当前登录用户在关联表的信息
            QueryWrapper<TeamUser> teamUserQueryWrapper = new QueryWrapper<>();
            User loginUser = userService.getLoginUser(request);
            teamUserQueryWrapper.eq("userId",loginUser.getId());
            teamUserQueryWrapper.in("teamId",teamIdList);
            //得到当前登录用户在关联表的信息
            List<TeamUser> teamUserList = teamUserService.list(teamUserQueryWrapper);
            //通过关联表消息得到登录用户加入的队伍的id
            Set<Long> hasJoinTeamIdSet = teamUserList.stream().map(TeamUser::getTeamId).collect(Collectors.toSet());
            //遍历传入的原队伍列表，通过用户加入队伍id的集合，标识用户加入了哪些队伍
            originTeamList.forEach(team ->{
                //判断当前队伍是否被用户加入
                boolean hasJoin = hasJoinTeamIdSet.contains(team.getId());
                team.setHasJoin(hasJoin);
            });
            /**
             * 查询已加入队伍的人数
             */
            QueryWrapper<TeamUser> userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.in("teamId",teamIdList);
            List<TeamUser> userTeamList = teamUserService.list(userTeamQueryWrapper);
            //根据teamId进行分组，分组后就知道每个队伍的人数了
            Map<Long, List<TeamUser>> teamIdUserTeamList = userTeamList.stream().
                    collect(Collectors.groupingBy(TeamUser::getTeamId));
            //给hasJoinNum设置值
            originTeamList.forEach(team ->
                    team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(),new
                            ArrayList<>()).size()));
        }catch (Exception e){

        }
        //将处理完的原队伍列表返回出去
        return originTeamList;

    }



}




