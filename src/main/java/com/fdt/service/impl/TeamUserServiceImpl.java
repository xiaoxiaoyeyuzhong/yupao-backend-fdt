package com.fdt.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fdt.model.domain.TeamUser;
import com.fdt.service.TeamUserService;
import com.fdt.mapper.TeamUserMapper;
import org.springframework.stereotype.Service;

/**
* @author 冯德田
* @description 针对表【team_user(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-08-15 17:35:34
*/
@Service
public class TeamUserServiceImpl extends ServiceImpl<TeamUserMapper, TeamUser>
    implements TeamUserService{

}




