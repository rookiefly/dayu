package com.rookiefly.open.dubbo.dayu.biz.service.impl;

import com.rookiefly.open.dubbo.dayu.biz.service.HostService;
import com.rookiefly.open.dubbo.dayu.biz.service.InvokeService;
import com.rookiefly.open.dubbo.dayu.common.redis.RedisClientTemplate;
import com.rookiefly.open.dubbo.dayu.common.redis.RedisKeyConstants;
import com.rookiefly.open.dubbo.dayu.common.tools.JsonUtil;
import com.rookiefly.open.dubbo.dayu.dao.mapper.InvokeDOMapper;
import com.rookiefly.open.dubbo.dayu.model.bo.HostBO;
import com.rookiefly.open.dubbo.dayu.model.bo.MethodRankBO;
import com.rookiefly.open.dubbo.dayu.model.entity.InvokeDO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class InvokeServiceImpl implements InvokeService {

    @Resource
    private HostService hostService;

    @Resource
    private RedisClientTemplate redisClientTemplate;

    @Resource(name = "invokeDOMapper")
    private InvokeDOMapper invokeDOMapper;

    /**
     * 排行榜展示最大的数量
     */
    private static final Integer MAX_RANK_NUMBER = 50;

    @Override
    public List<MethodRankBO> getMethodRankByAppName(String appName) {
        List<MethodRankBO> resultList = new ArrayList<>();
        if (StringUtils.isEmpty(appName)) {
            return resultList;
        }

        String redisKey = String.format(RedisKeyConstants.INVOKE_METHOD_RANK_KEY, appName);
        String redisResultString = redisClientTemplate.get(redisKey);
        if (redisResultString != null && redisClientTemplate.isNone(redisResultString)) {
            //缓存里判定之前查找为空，因此此次不走数据库，直接空
            return resultList;
        }
        if (redisResultString != null) {
            //返回redis 缓存结果集
            return JsonUtil.jsonStrToList(redisResultString, MethodRankBO.class);
        }
        //redis 中无数据，进行数据库操作
        resultList = findFromDataBase(appName);
        //缓存一份到数据库
        if (resultList.isEmpty()) {
            redisClientTemplate.setNone(redisKey);
        } else {
            redisClientTemplate.lazySet(redisKey, resultList, RedisKeyConstants.RREDIS_EXP_HOURS * 23);
        }
        return resultList;
    }

    private List<MethodRankBO> findFromDataBase(String appName) {
        List<MethodRankBO> resultList = new ArrayList<>();
        Set<HostBO> hostBOSet = hostService.getHostPortByAppName(appName);
        if (hostBOSet.isEmpty()) {
            return resultList;
        }

        List<InvokeDO> invokeDOList = new ArrayList<>();
        for (HostBO hostBO : hostBOSet) {
            //数据库拿出所有的数据，叠加到list
            String host = hostBO.getHost();
            String port = hostBO.getPort();

            InvokeDO searchDO = new InvokeDO();
            searchDO.setProviderHost(host);
            searchDO.setProviderPort(port);

            invokeDOList.addAll(invokeDOMapper.selectByInvokeDO(searchDO));
        }
        if (invokeDOList.isEmpty()) {
            return resultList;
        }
        // 存在数据
        Map<MethodRankBO, Integer> rankMap = new HashMap<>();
        for (InvokeDO invokeDO : invokeDOList) {
            String serviceName = invokeDO.getService();
            String methodName = invokeDO.getMethod();
            Integer usedNum = invokeDO.getSuccess();

            MethodRankBO rankBO = new MethodRankBO();
            rankBO.setServiceName(serviceName);
            rankBO.setMethodName(methodName);
            Integer nowNum = rankMap.get(rankBO);
            if (nowNum == null) nowNum = 0;
            nowNum += usedNum;
            rankMap.put(rankBO, nowNum);
        }
        //排序,从大到小
        List<Map.Entry<MethodRankBO, Integer>> sortedList = new ArrayList<>(rankMap.entrySet());
        sortedList.sort((o1, o2) -> o2.getValue() - o1.getValue());
        int sortedListSize = sortedList.size();
        for (int i = 0; i < sortedListSize; i++) {
            Map.Entry<MethodRankBO, Integer> entry = sortedList.get(i);
            MethodRankBO rankBO = entry.getKey();
            rankBO.setUsedNum(entry.getValue());
            resultList.add(rankBO);
            if (resultList.size() > MAX_RANK_NUMBER - 1) {
                break;
            }
        }
        return resultList;
    }
}
