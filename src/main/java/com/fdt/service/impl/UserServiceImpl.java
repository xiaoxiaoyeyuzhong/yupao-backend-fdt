package com.fdt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fdt.common.ErrorCode;
import com.fdt.exception.BusinessException;
import com.fdt.model.domain.User;
import com.fdt.service.UserService;
import com.fdt.mapper.UserMapper;
import com.fdt.utils.AlgorithmUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.fdt.contant.UserContant.ADMIN_ROLE;
import static com.fdt.contant.UserContant.USER_LOGIN_STATE;

/**
 * @author 冯德田
 * description 针对表【user(用户)】的数据库操作Service实现
 * createDate 2024-05-12 23:04:46
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    //读写redis需要的变量
    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    private ValueOperations<String, Object> valueOperations;
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "fdt";


    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 重复密码
     * @param planetCode    星球编号
     * @return long 用户id
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1. 校验
        // 不能为空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码为空");
        }
        //账户长度不能小于4
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能小于4位");
        }
        //密码和重复长度不能小于8
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能小于8位");
        }

        // 星球编号长度不大于5
        if (planetCode.length() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号长度不大于5位");
        }

        //账户不能包含特殊字符
        String validPattern = "^[a-zA-Z0-9]+$";
        if (!userAccount.matches(validPattern)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }

        // 密码和校验密码相同，不能用等于判断两个字符串是否相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和校验密码应相同");
        }

        // 账户不能重复，数据库操作放后面，避免性能浪费，
        // 如果其他规则都没通过，就不需要判断账户是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        // 星球编号不能重复
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号不能重复");
        }

        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        //3. 保存到数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "数据库保存用户注册信息失败");
        }
        return user.getId();
    }


    /**
     * 登录逻辑
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      请求
     * @return User 用户
     */
    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        // 不能为空
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码和校验密码应相同");
        }
        //账户长度不能小于4
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度要大于4");
        }
        //密码长度不能小于8
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度要大于8");
        }

        //账户不能包含特殊字符
        String validPattern = "^[a-zA-Z0-9]+$";
        if (!userAccount.matches(validPattern)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能包含特殊字符");
        }

        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        // 3. 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed,userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码错误");
        }


        //4.脱敏
        User safetyUser = getSafetyUser(user);

        //5.记录用户登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);

        return safetyUser;
    }

    /**
     * 获取脱敏用户
     *
     * @param originUser 原用户信息
     * @return 脱敏用户信息
     */
    @Override
    public User getSafetyUser(User originUser) {
        if (originUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户信息为空");
        }
        User safetyUser = new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setTags(originUser.getTags());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setStatus(originUser.getStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        return safetyUser;
    }

    /**
     * 根据标签获取用户
     * 在内存中进行过滤
     *
     * @param tagNameList 标签列表
     * @return List<User> 用户列表
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
//      首先判断传入的标签列表是否为空
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //    2使用内存查询
//        2.1 先查询所有用户
//        查询开始时间
//        long startTime = System.currentTimeMillis();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
//        2.2 在内存中判断判断是否包含传入的标签
//        使用了lambda表达式 -> 指向动作
        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
//            如果tagsStr为空，用户不存在标签，就无法通过标签查询用户，返回false
            if (StringUtils.isBlank(tagsStr)) {
                return false;
            }
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
//          判断tempTagNameSet是否为空，java 8特性。
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for (String tageName : tagNameList) {
                if (!tempTagNameSet.contains(tageName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
//        查询结束时间
//        log.info("内存查询用户耗时："+(System.currentTimeMillis() - startTime)+"ms");
    }

    /**
     * 根据标签获取用户
     * SQL查询版
     *
     * @param tagNameList
     * @return List<User>
     * private 设置成私有防止调用
     * @Deprecated 废弃接口
     */
    @Deprecated
    private List<User> SQLsearchUsersByTags(List<String> tagNameList) {
//      首先判断传入的标签列表是否为空
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//      查询开始时间
//      long startTime = System.currentTimeMillis();
//      使用SQL查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//      1.拼接 and 查询，形式 like '%java%' and like '%python%'
        for (String tagName : tagNameList) {
//      1.2 使用like匹配tags列是否包含tagName
            queryWrapper = queryWrapper.like("tags", tagName);
        }
//      1.3 调用自带的selectList方法，将定义好的查询条件传入
        List<User> userList = userMapper.selectList(queryWrapper);
        /*
        1.4 stream流式处理
           map方法将每个User对象转换为safetyUser对象
           collect方法将处理后的结果收集到一个新的List中
        *  */
        List<User> tempuserList = userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
//        查询结束时间
//        log.info("内存查询用户耗时："+(System.currentTimeMillis() - startTime)+"ms");
        return tempuserList;
//        查询结束时间
//        log.info("内存查询用户耗时："+(System.currentTimeMillis() - startTime)+"ms");
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public int userLogout(HttpServletRequest request) {
//        移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 更新用户信息
     */
    @Override
    public int updateUser(User user, User loginUser) {
        //仅管理员和自己可以修改用户信息
        long userId = user.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //如果是管理员，允许更新任意用户
        //如果不是管理员，只允许更新自己的信息
        if (!isAdmin(loginUser) && userId != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User oldUser = userMapper.selectById(userId);
        //判断要更新的用户是否存在，不存在返回请求的数据为空
        if (oldUser == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //调用根据id进行更新的方法
        return userMapper.updateById(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求信息
     * @return User 当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //如果请求信息为空，直接返回null
        if (request == null) {
            return null;
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        return (User) userObj;
    }

    /**
     * 是否为管理员
     *
     * @param request 请求信息
     * @return boolean
     * @author fdt
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 判断用户是否为管理员
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 是否为管理员
     *
     * @param loginUser 登录用户
     * @return boolean
     */
    @Override
    public boolean isAdmin(User loginUser) {
        // 判断用户是否为管理员
        boolean result = loginUser != null && loginUser.getUserRole() == ADMIN_ROLE;
        return result;
    }

    /**
     * 数据缓存，将键值对存入Redis中
     *
     * @param key   键
     * @param value 值
     * @param time  过期时间
     * @param unit  时间单位
     */
    @Override
    public void setRedisCache(String key, Object value, long time, TimeUnit unit) {
        if (valueOperations == null) {
            valueOperations = redisTemplate.opsForValue();
        }
        try {
            valueOperations.set(key, value, time, unit);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
    }

    @Override
    public Object getRedisCache(String key) {
        if (valueOperations == null) {
            valueOperations = redisTemplate.opsForValue();
        }
        try {
            return valueOperations.get(key);
        } catch (Exception e) {
            log.error("redis get key error", e);
        }
        return null;
    }

    /**
     * 获取最匹配用户的列表
     *
     * @param num       要获取的数量
     * @param loginUser 登录用户信息
     * @return List<User> 匹配用户列表
     */
    @Override
    public List<User> matchUsers(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //只需要id和tags列就行，提高查询性能
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        //获取标签不为空的用户列表，排除空的，减少无用数据
        List<User> userList = userMapper.selectList(queryWrapper);
        //获取登录用户的标签
        String tags = loginUser.getTags();
        //将json格式标签转为java对象
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        //记录用户下标及编辑距离，距离越短，相似度越高
        List<Pair<User,Long>> list = new ArrayList<>();
        //遍历所有用户，计算与当前登录用户的编辑距离
        for (User user : userList) {
            //获取用户标签
            String userTags = user.getTags();
            //再次校验用户标签是否存在及是否遍历到当前登录用户
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()) {
                //如果标签为空（isBlank）或为当前登录用户，不做任何处理
                continue;
            }
            //将用户标签转为java对象
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //调用计算相似度的方法，计算编辑距离
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
            //将用户和编辑距离添加到list中
            list.add(new Pair<>(user, distance));
        }
        //按编辑距离由小到大排序
        List<Pair<User,Long>> topUserPairList = list.stream()
                                                    .sorted((a,b)->(int)(a.getValue()-b.getValue())) //编辑距离两两比较
                                                    .limit(num) //限制条数
                                                    .collect(Collectors.toList());
        //从topUserPairList中取出用户id作为下标，分数作为值，为下面根据id查询最匹配用户列表后的排序做准备
        List<Long> userIdList = topUserPairList.stream().map(pair->pair
                .getKey().getId()).collect(Collectors.toList());
        //获取用户的所有信息，并进行脱敏
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id",userIdList);
        Map<Long,List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream().map(this::getSafetyUser)
                .collect(Collectors.groupingBy(User::getId));
        //根据userIdList进行排序
        List<User> finalUserList = new ArrayList<>();
        for(Long userId:userIdList){
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
    }
}




