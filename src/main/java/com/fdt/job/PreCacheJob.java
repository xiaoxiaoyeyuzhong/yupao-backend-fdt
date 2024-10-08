package com.fdt.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fdt.model.domain.User;
import com.fdt.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


//定时任务，缓存推荐用户 todo 缓存重点用户的推荐用户
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    //重点用户
    private List<Long> mainUserList = Arrays.asList(1L);

    //每天执行一次，预热推荐用户
    @Scheduled(cron = "0 49 15 * * *")
    public void doCacheRecommendUser() {
        RLock lock = redissonClient.getLock("yupao:precachejob:docache:lock");
        try {
            if(lock.tryLock(0,-1,TimeUnit.MILLISECONDS)){
                log.info("开始预热推荐用户");
                for (Long userId : mainUserList) {
                    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                    Page<User> userPage = userService.page(new Page<>(1,20),queryWrapper);
                    String redisKey = String.format("yupao:user:recommend:%s", userId);
                    userService.setRedisCache(redisKey, userPage, 30L, TimeUnit.MINUTES);
                }
                log.info("推荐用户预热完毕");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //锁的释放不能放try，要保证手动释放一定执行，防止看门狗机制一直续期
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
        }

    }
}
