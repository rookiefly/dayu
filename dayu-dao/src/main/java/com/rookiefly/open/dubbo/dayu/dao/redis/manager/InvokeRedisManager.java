package com.rookiefly.open.dubbo.dayu.dao.redis.manager;

import com.rookiefly.open.dubbo.dayu.common.redis.RedisClientTemplate;
import com.rookiefly.open.dubbo.dayu.common.redis.RedisKeyConstants;
import com.rookiefly.open.dubbo.dayu.common.tools.JsonUtil;
import com.rookiefly.open.dubbo.dayu.dao.mapper.InvokeDOMapper;
import com.rookiefly.open.dubbo.dayu.model.entity.InvokeDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * invoke核心数据存储类
 */
@Slf4j
@Service
public class InvokeRedisManager {

    @Resource
    private RedisClientTemplate redisClientTemplate;

    @Resource(name = "invokeDOMapper")
    private InvokeDOMapper invokeDOMapper;

    /**
     * 存 invoke,有效期是1天
     *
     * @param hour
     * @param invokeDO
     */
    public void saveInvoke(String hour, InvokeDO invokeDO) {
        String key = String.format(RedisKeyConstants.INVOKE_LIST_HOUR, hour);
        String jsonString = JsonUtil.objectToJsonStr(invokeDO);

        // 存储原始记录 2小时
        redisClientTemplate.rPushList(key, jsonString, RedisKeyConstants.RREDIS_EXP_HOURS * 2);
    }

    /**
     * 根据小时 获得缓存中的数据
     *
     * @param hour
     * @return
     */
    public List<InvokeDO> getInvokeByHour(String hour) {
        List<InvokeDO> resultList = new ArrayList<>();
        String key = String.format(RedisKeyConstants.INVOKE_LIST_HOUR, hour);

        List<String> jsonList = redisClientTemplate.getList(key, 0, -1);

        if (!jsonList.isEmpty()) {
            for (String jsonString : jsonList) {
                InvokeDO invokeDO = JsonUtil.jsonStrToObject(jsonString, InvokeDO.class);
                resultList.add(invokeDO);
            }
        }
        return resultList;
    }

    /**
     * 根据service,method,day 获得数据列表
     *
     * @param service
     * @param method
     * @param timeDate
     * @return
     */
    public List<InvokeDO> getInvokeByMethodDay(String service, String method, String timeDate) {
        String key = String.format(RedisKeyConstants.INVOKE_METHOD_DAY_KEY, service, method, timeDate);

        List<InvokeDO> resultList = new ArrayList<>();

        String resultJson = redisClientTemplate.get(key);
        if (redisClientTemplate.isNone(resultJson)) {
            return resultList;
        }

        if (null == resultJson) {
            InvokeDO searchDo = new InvokeDO();
            searchDo.setService(service);
            searchDo.setMethod(method);
            searchDo.setInvokeDate(timeDate);

            resultList = invokeDOMapper.selectByInvokeDO(searchDo);
            if (resultList.isEmpty()) {
                redisClientTemplate.setNone(key);
            } else {
                redisClientTemplate.lazySet(key, resultList, RedisKeyConstants.RREDIS_EXP_HOURS);
            }
        } else {
            resultList = redisClientTemplate.lazyGetList(key, InvokeDO.class);
        }

        return resultList;
    }
}
