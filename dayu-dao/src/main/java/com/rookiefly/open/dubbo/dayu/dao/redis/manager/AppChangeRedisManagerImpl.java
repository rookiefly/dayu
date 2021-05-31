package com.rookiefly.open.dubbo.dayu.dao.redis.manager;

import com.rookiefly.open.dubbo.dayu.common.redis.RedisClientTemplate;
import com.rookiefly.open.dubbo.dayu.common.redis.RedisKeyConstants;
import com.rookiefly.open.dubbo.dayu.common.tools.JsonUtil;
import com.rookiefly.open.dubbo.dayu.common.tools.TimeUtil;
import com.rookiefly.open.dubbo.dayu.model.bo.ApplicationChangeBO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用服务发生变化后的处理
 */
@Service
public class AppChangeRedisManagerImpl implements AppChangeRedisManager {

    private Boolean recentDeleteNumOk = false;

    private Boolean recentInsertNumOk = false;

    /**
     * 保存过的日期
     */
    private static final List<String> HAVE_DAY_LIST = new ArrayList<>();

    /**
     * 最近常用保持30条记录
     */
    private static final Integer RECENT_NUM = 30;

    @Resource
    private RedisClientTemplate redisClientTemplate;

    /**
     * 获得最近新增30条
     **/
    @Override
    public List<ApplicationChangeBO> getRecentInsertList() {
        List<ApplicationChangeBO> resultList = new ArrayList<>();
        List<String> list = redisClientTemplate.getList(RedisKeyConstants.RECENT_INSERT_KEY, 0, -1);
        for (String recentString : list) {
            ApplicationChangeBO applicationChangeBO = JsonUtil.jsonStrToObject(recentString, ApplicationChangeBO.class);
            resultList.add(applicationChangeBO);
        }
        return resultList;
    }

    /**
     * 获得最近减少30条
     **/
    @Override
    public List<ApplicationChangeBO> getRecentDeleteList() {
        List<ApplicationChangeBO> resultList = new ArrayList<>();
        List<String> list = redisClientTemplate.getList(RedisKeyConstants.RECENT_DELETE_KEY, 0, -1);
        for (String recentString : list) {
            ApplicationChangeBO applicationChangeBO = JsonUtil.jsonStrToObject(recentString, ApplicationChangeBO.class);
            resultList.add(applicationChangeBO);
        }
        return resultList;
    }


    /**
     * 按日期获得数据
     **/
    @Override
    public List<ApplicationChangeBO> getChangeListByDay(String day, Integer pageIndex, Integer limit) {
        List<ApplicationChangeBO> resultList = new ArrayList<>();
        String thisDayKey = String.format(RedisKeyConstants.DAY_CHANGE_KEY, day);

        Integer start = (pageIndex - 1) * limit;
        Integer end = start + limit - 1;
        List<String> list = redisClientTemplate.getList(thisDayKey, start, end);
        for (String recentString : list) {
            ApplicationChangeBO applicationChangeBO = JsonUtil.jsonStrToObject(recentString, ApplicationChangeBO.class);
            resultList.add(applicationChangeBO);
        }
        return resultList;
    }

    @Override
    public Integer getListSum(String day) {
        String thisDayKey = String.format(RedisKeyConstants.DAY_CHANGE_KEY, day);

        return redisClientTemplate.listSize(thisDayKey);
    }

    //按月份获得实际日期
    @Override
    public Set<String> getDaySet(String month) {
        String monthKey = String.format(RedisKeyConstants.MONTH_DAY_KEY, month);
        return redisClientTemplate.getSet(monthKey);
    }

    @Override
    public Map<String, List<Map<String, String>>> getChangeAppCache() {
        String key = RedisKeyConstants.APP_CHANGE_CACHE_KEY;

        return redisClientTemplate.lazyGet(key, ConcurrentHashMap.class);
    }

    @Override
    public void saveChangeAppCache(Map<String, Set<ApplicationChangeBO>> map) {
        String key = RedisKeyConstants.APP_CHANGE_CACHE_KEY;
        redisClientTemplate.lazySet(key, map, null);
    }

    @Override
    public void addDeleteRecentRecord(ApplicationChangeBO applicationChangeBO) {
        Date now = new Date();
        String thisDay = TimeUtil.getDateString(now);
        String deleteString = JsonUtil.objectToJsonStr(applicationChangeBO);

        //最近常用删除位
        String recentDeleteKey = RedisKeyConstants.RECENT_DELETE_KEY;
        redisClientTemplate.lPushList(recentDeleteKey, deleteString);


        Integer deleteSize = redisClientTemplate.listSize(recentDeleteKey);
        Integer diff = 0;
        if (deleteSize >= RECENT_NUM) {
            recentDeleteNumOk = true;
            diff = deleteSize - RECENT_NUM;
        }
        if (recentDeleteNumOk) {
            for (int i = 0; i < diff; i++) {
                redisClientTemplate.rPopList(recentDeleteKey);
            }
        }
        //本日删除位记录
        String thisDayKey = String.format(RedisKeyConstants.DAY_CHANGE_KEY, thisDay);
        redisClientTemplate.lPushList(thisDayKey, deleteString, RedisKeyConstants.RREDIS_EXP_WEEK);

        if (!HAVE_DAY_LIST.contains(thisDay)) {
            String thisMonth = TimeUtil.getYearMonthString(now);
            String monthKey = String.format(RedisKeyConstants.MONTH_DAY_KEY, thisMonth);
            redisClientTemplate.addSet(monthKey, thisDay, RedisKeyConstants.RREDIS_EXP_WEEK);
            HAVE_DAY_LIST.add(thisDay);
        }
    }

    @Override
    public void addInsertRecentRecord(ApplicationChangeBO applicationChangeBO) {
        Date now = new Date();
        String thisDay = TimeUtil.getDateString(now);
        String insertString = JsonUtil.objectToJsonStr(applicationChangeBO);

        //最近常用insert位
        String recentInsertKey = RedisKeyConstants.RECENT_INSERT_KEY;
        redisClientTemplate.lPushList(recentInsertKey, insertString);

        Integer insertSize = redisClientTemplate.listSize(recentInsertKey);
        Integer diff = 0;
        if (insertSize >= RECENT_NUM) {
            recentInsertNumOk = true;
            diff = insertSize - RECENT_NUM;
        }
        if (recentInsertNumOk) {
            for (int i = 0; i < diff; i++) {
                redisClientTemplate.rPopList(recentInsertKey);
            }
        }
        //本月insert位记录
        String thisDayKey = String.format(RedisKeyConstants.DAY_CHANGE_KEY, thisDay);
        redisClientTemplate.lPushList(thisDayKey, insertString, RedisKeyConstants.RREDIS_EXP_WEEK);
        if (!HAVE_DAY_LIST.contains(thisDay)) {
            String thisMonth = TimeUtil.getYearMonthString(now);
            String monthKey = String.format(RedisKeyConstants.MONTH_DAY_KEY, thisMonth);
            redisClientTemplate.addSet(monthKey, thisDay, RedisKeyConstants.RREDIS_EXP_WEEK);
            HAVE_DAY_LIST.add(thisDay);
        }
    }
}
