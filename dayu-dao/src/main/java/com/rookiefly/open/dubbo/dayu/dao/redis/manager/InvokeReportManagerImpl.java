package com.rookiefly.open.dubbo.dayu.dao.redis.manager;

import com.rookiefly.open.dubbo.dayu.common.redis.RedisClientTemplate;
import com.rookiefly.open.dubbo.dayu.common.redis.RedisKeyBean;
import com.rookiefly.open.dubbo.dayu.common.tools.JsonUtil;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class InvokeReportManagerImpl implements InvokeReportManager {

    @Resource
    private RedisClientTemplate redisClientTemplate;

    @Override
    public void saveAppRelationByAppOnDay(String sourceApp, String dayTime, Map<String, Map<String, Integer>> reportMap) {
        String key = String.format(RedisKeyBean.appInvokeSumOnDayKEY, sourceApp, dayTime);
        Map<String, String> saveMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : reportMap.entrySet()) {
            Map<String, Integer> valueMap = entry.getValue();
            String valueString = JsonUtil.objectToJsonStr(valueMap);
            saveMap.put(entry.getKey(), valueString);
        }
        redisClientTemplate.setMap(key, saveMap, RedisKeyBean.RREDIS_EXP_DAY * 15);
    }

    @Override
    public Map<String, Map<String, Integer>> getAppRelationByAppOnDay(String sourceApp, String dayTime) {
        String key = String.format(RedisKeyBean.appInvokeSumOnDayKEY, sourceApp, dayTime);
        Map<String, String> redisMap = redisClientTemplate.getAllHash(key);

        Map<String, Map<String, Integer>> result = new HashMap<>();
        if (redisMap.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String> entry : redisMap.entrySet()) {
            String valueString = entry.getValue();
            Map<String, Integer> valueMap = JsonUtil.jsonStrToMap(valueString);
            result.put(entry.getKey(), valueMap);
        }
        return result;
    }

    @Override
    public void saveConsumerByAppOnHour(String sourceApp, String dayTime, Map<String, Map<String, ?>> reportMap) {
        String key = String.format(RedisKeyBean.appConsumerSumOnHourKEY, sourceApp, dayTime);
        Map<String, String> saveMap = new HashMap<>();
        for (Map.Entry<String, Map<String, ?>> entry : reportMap.entrySet()) {
            Map<String, ?> valueMap = entry.getValue();
            String valueString = JsonUtil.objectToJsonStr(valueMap);
            saveMap.put(entry.getKey(), valueString);
        }
        redisClientTemplate.setMap(key, saveMap, RedisKeyBean.RREDIS_EXP_DAY * 2);
    }

    @Override
    public Map<String, ?> getConsumerByAppOnHour(String sourceApp, String dayTime) {
        String key = String.format(RedisKeyBean.appConsumerSumOnHourKEY, sourceApp, dayTime);
        Map<String, String> redisMap = redisClientTemplate.getAllHash(key);

        Map<String, Map<String, ?>> result = new HashMap<>();
        if (redisMap.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String> entry : redisMap.entrySet()) {
            String valueString = entry.getValue();
            Map<String, ?> valueMap = JsonUtil.jsonStrToMap(valueString);
            result.put(entry.getKey(), valueMap);
        }
        return result;
    }

    @Override
    public void saveConsumerByAppOnDay(String sourceApp, String dayTime, Map<String, Map<String, Integer>> reportMap) {
        String key = String.format(RedisKeyBean.appConsumerSumOnDayKEY, sourceApp, dayTime);
        Map<String, String> saveMap = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : reportMap.entrySet()) {
            Map<String, ?> valueMap = entry.getValue();
            String valueString = JsonUtil.objectToJsonStr(valueMap);
            saveMap.put(entry.getKey(), valueString);
        }
        redisClientTemplate.setMap(key, saveMap, RedisKeyBean.RREDIS_EXP_DAY * 15);
    }

    @Override
    public Map<String, Map<String, Integer>> getConsumerByAppOnDay(String sourceApp, String dayTime) {
        String key = String.format(RedisKeyBean.appConsumerSumOnDayKEY, sourceApp, dayTime);
        Map<String, String> redisMap = redisClientTemplate.getAllHash(key);

        Map<String, Map<String, Integer>> result = new HashMap<>();
        if (redisMap.isEmpty()) {
            return result;
        }
        for (Map.Entry<String, String> entry : redisMap.entrySet()) {
            String valueString = entry.getValue();
            Map<String, Integer> valueMap = JsonUtil.jsonStrToMap(valueString);
            result.put(entry.getKey(), valueMap);
        }
        return result;
    }
}
