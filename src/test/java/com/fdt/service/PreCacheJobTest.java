package com.fdt.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fdt.model.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class PreCacheJobTest
{

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    //重点用户
    private List<Long> mainUserList = Arrays.asList(1L);

    @Test
    public void test(){
        RList<String> rList = redissonClient.getList("test-lis");
        rList.add(0,"12432235235432");
        rList.add(1,"yupi");
//        rList.add("dog");
//        System.out.println(rList.get(0));
//        System.out.println(rList.get(1));

        System.out.println(rList.get(0));
        System.out.println(rList.get(1));
        System.out.println(rList.size());
//        rList.remove(0);
        rList.remove(0);
        rList.remove(1);


    }
    @Test
    public void testPreCache(){
        RLock lock = redissonClient.getLock("yupao:precachejob:docache:lock");
        try {
            if(lock.tryLock(0,-1, TimeUnit.MILLISECONDS)){
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

