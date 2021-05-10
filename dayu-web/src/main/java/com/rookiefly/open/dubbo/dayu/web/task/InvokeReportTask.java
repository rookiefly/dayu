package com.rookiefly.open.dubbo.dayu.web.task;

import com.rookiefly.open.dubbo.dayu.biz.service.ApplicationService;
import com.rookiefly.open.dubbo.dayu.biz.service.HostService;
import com.rookiefly.open.dubbo.dayu.biz.service.InvokeService;
import com.rookiefly.open.dubbo.dayu.common.constants.MonitorConstants;
import com.rookiefly.open.dubbo.dayu.common.tools.TimeUtil;
import com.rookiefly.open.dubbo.dayu.dao.redis.manager.InvokeRedisManager;
import com.rookiefly.open.dubbo.dayu.dao.redis.manager.InvokeReportManager;
import com.rookiefly.open.dubbo.dayu.model.bo.HostBO;
import com.rookiefly.open.dubbo.dayu.model.entity.InvokeDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 报表数据，每小时统计一次开始统计
 */
@Component
@Slf4j
public class InvokeReportTask {

    @Resource
    private InvokeReportManager invokeReportManager;

    @Resource
    private InvokeRedisManager invokeRedisManager;

    @Resource
    private InvokeService invokeBiz;

    @Resource
    private ApplicationService applicationService;

    @Resource
    private HostService hostService;

    /**
     * 每天每个小时小时 :01
     */
    @Scheduled(cron = "0 1 * * * ?")
    public void everyHourDo() {
        //应用间调用数量
        try {
            appSumOnHour();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //应用作为提供者 每小时被消费的数量
        try {
            appConsumerHourInHour();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 每天凌晨 00：01分执行
     */
    @Scheduled(cron = "0 1 0 * * ?")
    public void everyDayDo() {
        //应用作为提供者 每天被消费的数量
        try {
            appConsumerOnHourToDay();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //应用方法排行榜
        try {
            appMethodRankOnDay();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 每天每个小时小时 :01 统计每个应用每个小时相互调用情况
     */
    private void appSumOnHour() {
        Date now = new Date();
        Date lastHourDate = TimeUtil.getBeforHourByNumber(now, -1);
        String lastHourDay = TimeUtil.getDateString(now);
        String lastHour = TimeUtil.getHourString(lastHourDate);

        List<String> allApplication = applicationService.getAllApplicationsCache();


        List<InvokeDO> invokeDOList = invokeRedisManager.getInvokeByHour(lastHour);

        for (String applicationName : allApplication) {
            Map<String, Map<String, Integer>> appDayMap = invokeReportManager.getAppRelationByAppOnDay(applicationName, lastHourDay);
            Map<String, Integer> providerMap = appDayMap.get(CommonConstants.PROVIDER);
            Map<String, Integer> consumerMap = appDayMap.get(CommonConstants.CONSUMER);

            if (providerMap == null) {
                providerMap = new HashMap<>();
                appDayMap.put(CommonConstants.PROVIDER, providerMap);
            }
            if (consumerMap == null) {
                consumerMap = new HashMap<>();
                appDayMap.put(CommonConstants.CONSUMER, consumerMap);
            }

            boolean hasPro = false;
            boolean hasConsu = false;
            for (InvokeDO invokeDO : invokeDOList) {
                String invokeType = invokeDO.getAppType();
                if (invokeType.equals(CommonConstants.PROVIDER)) {
                    // 做为提供者，提供服务--找不到消费者
                    continue;
                }
                String providerHost = invokeDO.getProviderHost();
                String providerPort = invokeDO.getProviderPort();
                Set<String> nameSet = hostService.getAppNameByHost(new HostBO(providerHost, providerPort));
                if (nameSet.size() != 1) {
                    // 有且只有一个
                    continue;
                }
                String appName = invokeDO.getApplication();
                String providerName = nameSet.iterator().next();
                if (applicationName.equals(appName)) {
                    Integer success = invokeDO.getSuccess();
                    // app 作为消费者，被提供
                    Integer providerSum = providerMap.get(providerName) == null ? Integer.valueOf(0) : providerMap.get(providerName);
                    providerSum += success;
                    providerMap.put(providerName, providerSum);
                    hasPro = true;
                }
                if (applicationName.equals(providerName)) {
                    // app 作为提供者，被消费
                    Integer success = invokeDO.getSuccess();
                    Integer consumerSum = consumerMap.get(appName) == null ? Integer.valueOf(0) : consumerMap.get(appName);
                    consumerSum += success;
                    consumerMap.put(appName, consumerSum);
                    hasConsu = true;
                }
            }
            if (!hasPro) {
                appDayMap.remove(CommonConstants.PROVIDER);
            }
            if (!hasConsu) {
                appDayMap.remove(CommonConstants.CONSUMER);
            }

            if (hasConsu || hasPro) {
                invokeReportManager.saveAppRelationByAppOnDay(applicationName, lastHourDay, appDayMap);
            }
        }
    }

    /**
     * 每小时的数据调用
     */
    private void appConsumerHourInHour() {
        Date now = new Date();
        Date lastHourDate = TimeUtil.getBeforHourByNumber(now, -1);
        String lastHourDay = TimeUtil.getDateString(now);
        String lastHour = TimeUtil.getHourString(lastHourDate);

        List<String> allApplication = applicationService.getAllApplicationsCache();

        List<InvokeDO> invokeDOList = invokeRedisManager.getInvokeByHour(lastHour);

        for (String applicationName : allApplication) {
            Map<String, Map<String, ?>> saveMap = (Map<String, Map<String, ?>>) invokeReportManager.getConsumerByAppOnHour(applicationName, lastHourDay);

            boolean isOk = false;

            for (InvokeDO invokeDO : invokeDOList) {
                String invokeType = invokeDO.getAppType();
                if (invokeType.equals(CommonConstants.PROVIDER)) {
                    // 做为提供者，提供服务--找不到消费者
                    continue;
                }
                String providerHost = invokeDO.getProviderHost();
                String providerPort = invokeDO.getProviderPort();
                Set<String> nameSet = hostService.getAppNameByHost(new HostBO(providerHost, providerPort));
                if (nameSet.size() != 1) {
                    // 有且只有一个
                    continue;
                }
                String appName = invokeDO.getApplication();
                String providerName = nameSet.iterator().next();
                if (applicationName.equals(providerName)) {
                    isOk = true;
                    // app 作为提供者，被消费
                    Integer success = invokeDO.getSuccess();
                    Integer fail = invokeDO.getFailure();
                    // 存储
                    Map<String, Object> hourSumMap = (Map<String, Object>) saveMap.get(appName);
                    if (null == hourSumMap) {
                        hourSumMap = new HashMap<>();
                        saveMap.put(appName, hourSumMap);
                    }
                    Map<String, Integer> sumMap = (Map<String, Integer>) hourSumMap.get(lastHour);
                    if (sumMap == null) {
                        sumMap = new HashMap<>();
                        sumMap.put(MonitorConstants.SUCCESS, success);
                        sumMap.put(MonitorConstants.FAIL, fail);
                        hourSumMap.put(lastHour, sumMap);
                    } else {
                        success += sumMap.get(MonitorConstants.SUCCESS);
                        fail += sumMap.get(MonitorConstants.FAIL);
                        sumMap.put(MonitorConstants.SUCCESS, success);
                        sumMap.put(MonitorConstants.FAIL, fail);
                    }
                }
            }

            if (isOk) {
                invokeReportManager.saveConsumerByAppOnHour(applicationName, lastHourDay, saveMap);
            }
        }
    }

    /**
     * 每天凌晨 00:01 统计每个应用昨天的每小时消费者消费情况，汇总为一天
     */
    private void appConsumerOnHourToDay() {
        String yesterday = TimeUtil.getBeforDateByNumber(new Date(), -1);
        List<String> allApplication = applicationService.getAllApplicationsCache();

        for (String applicationName : allApplication) {
            Map<String, Map<String, ?>> saveMap = (Map<String, Map<String, ?>>) invokeReportManager.getConsumerByAppOnHour(applicationName, yesterday);

            Map<String, Map<String, Integer>> dayMap = new HashMap<>();

            for (Map.Entry<String, Map<String, ?>> mapEntry : saveMap.entrySet()) {
                String consumerApp = mapEntry.getKey();
                Map<String, ?> hourSumMap = mapEntry.getValue();

                boolean isOk = false;
                Integer success = 0;
                Integer fail = 0;
                for (Map.Entry<String, ?> hourEntry : hourSumMap.entrySet()) {
                    Map<String, Integer> sumMap = (Map<String, Integer>) hourEntry.getValue();

                    success += sumMap.get(MonitorConstants.SUCCESS);
                    fail += sumMap.get(MonitorConstants.FAIL);
                    isOk = true;
                }

                if (isOk) {
                    // 存当日 sourceAPP 被 consumerApp 消费的成功数
                    Map<String, Integer> numberMap = new HashMap<>();
                    numberMap.put(MonitorConstants.SUCCESS, success);
                    numberMap.put(MonitorConstants.FAIL, fail);
                    dayMap.put(consumerApp, numberMap);
                }

            }
            if (!dayMap.isEmpty()) {
                invokeReportManager.saveConsumerByAppOnDay(applicationName, yesterday, dayMap);
            }
        }
    }

    /**
     * 每天凌晨统计之前一天的每个应用排行榜
     */
    private void appMethodRankOnDay() {
        List<String> allApplication = applicationService.getAllApplicationsCache();
        for (String applicationName : allApplication) {
            invokeBiz.getMethodRankByAppName(applicationName);
        }
    }
}