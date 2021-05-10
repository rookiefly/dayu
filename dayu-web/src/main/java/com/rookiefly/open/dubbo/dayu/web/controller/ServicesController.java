package com.rookiefly.open.dubbo.dayu.web.controller;

import com.rookiefly.open.dubbo.dayu.biz.service.HostService;
import com.rookiefly.open.dubbo.dayu.biz.service.ServicesService;
import com.rookiefly.open.dubbo.dayu.common.tools.TimeUtil;
import com.rookiefly.open.dubbo.dayu.dao.redis.manager.InvokeRedisManager;
import com.rookiefly.open.dubbo.dayu.common.constants.MonitorConstants;
import com.rookiefly.open.dubbo.dayu.model.vo.ResultVO;
import com.rookiefly.open.dubbo.dayu.model.bo.HostBO;
import com.rookiefly.open.dubbo.dayu.model.bo.ServiceBO;
import com.rookiefly.open.dubbo.dayu.model.entity.InvokeDO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/monitor/services")
public class ServicesController {

    @Resource
    private ServicesService servicesService;

    @Resource
    private InvokeRedisManager invokeRedisManager;

    @Resource
    private HostService hostService;

    @RequestMapping(value = "main", method = RequestMethod.GET)
    public ModelAndView main() {
        return new ModelAndView("/services/servicesIndex");
    }

    @RequestMapping(value = "/getAllService", method = RequestMethod.GET)
    @ResponseBody
    public ResultVO getAllService() {
        try {
            Map<String, Object> resultMap = new HashMap<>();

            Map<String, ServiceBO> allServicesMap = servicesService.getServiceBOMap();
            List<String> wrongMethodsList = new ArrayList<>();
            List<String> wrongAppList = new ArrayList<>();
            List<String> wrongHostServiceList = new ArrayList<>();

            for (Map.Entry<String, ServiceBO> serviceBOEntry : allServicesMap.entrySet()) {
                String serviceName = serviceBOEntry.getKey();
                ServiceBO serviceBO = serviceBOEntry.getValue();

                Set<String> ownerSet = serviceBO.getOwnerApp();
                Set<String> methodSet = serviceBO.getMethods();
                if (null != ownerSet && ownerSet.size() > 1) {
                    wrongAppList.add(serviceName);
                }
                if (null != methodSet && methodSet.size() > 1) {
                    wrongMethodsList.add(serviceName);
                }
                if (serviceBO.getIsHostWrong()) {
                    wrongHostServiceList.add(serviceName);
                }
            }

            //测试环境url
            Set<String> testUrlSet = new HashSet<>();
            for (Map.Entry<String, String> entry : MonitorConstants.ECS_TEST_MAP.entrySet()) {
                testUrlSet.add(entry.getKey());
                testUrlSet.add(entry.getValue());
            }

            resultMap.put("wrongAppList", wrongAppList);
            resultMap.put("wrongMethodsList", wrongMethodsList);
            resultMap.put("wrongHostServiceList", wrongHostServiceList);
            resultMap.put("allServicesMap", allServicesMap);
            resultMap.put("testUrlSet", testUrlSet);

            return ResultVO.wrapSuccessfulResult(resultMap);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.wrapErrorResult(e.getMessage());
        }
    }

    /**
     * 获得此service下的 方法 当前时间的调用量
     *
     * @param serviceName
     * @param methodName
     * @param type
     * @return
     */
    @RequestMapping(value = "/getMethodSumOneDay", method = RequestMethod.GET)
    @ResponseBody
    public ResultVO getMethodSumOneDay(String serviceName, String methodName, String type) {
        List<String> recentDateList = getRecentDay(type);
        //判断 host 是否有这个 service
        List<String> haveList = new ArrayList<>();
        List<String> notHaveList = new ArrayList<>();

        Map<String, Object> resultMap = new HashMap<>();
        //消费的app
        Set<String> appList = new HashSet<>();

//        {hour:{success:xxx,elapsed:xxx}}
        Map<String, Object> dataMap = new HashMap<>();
        for (String date : recentDateList) {
            List<InvokeDO> methodList = invokeRedisManager.getInvokeByMethodDay(serviceName.split(":")[0], methodName, date);
            for (InvokeDO invokeDO : methodList) {
                String providerHost = invokeDO.getProviderHost();
                String providerPort = invokeDO.getProviderPort();
                String providerKey = providerHost + "-" + providerPort;

                if (notHaveList.contains(providerKey)) {
                    // 非此service
                    continue;
                }
                if (!haveList.contains(providerKey)) {
                    Set<String> serviceSet = hostService.getServiceByHost(new HostBO(providerHost, providerPort));
                    if (!serviceSet.contains(serviceName)) {
                        // 非此service
                        notHaveList.add(providerKey);
                        continue;
                    }
                    haveList.add(providerKey);
                }

                appList.add(invokeDO.getApplication());
                Integer successNum = invokeDO.getSuccess();
                Integer elapsedNum = invokeDO.getElapsed();

                String hourTime = invokeDO.getInvokeHour();

                Map<String, Integer> hourMap = (Map<String, Integer>) dataMap.get(hourTime);
                if (hourMap == null) {
                    hourMap = new HashMap<>();
                    dataMap.put(hourTime, hourMap);
                }
                Integer oldSuccessNum = hourMap.get("success") == null ? 0 : hourMap.get("success");
                Integer oldElapsedNum = hourMap.get("elapsed") == null ? 0 : hourMap.get("elapsed");

                hourMap.put("success", oldSuccessNum + successNum);
                hourMap.put("elapsed", oldElapsedNum + elapsedNum);
            }
        }
        resultMap.put("dataMap", dataMap);
        resultMap.put("appList", appList);

        return ResultVO.wrapSuccessfulResult(resultMap);
    }

    private List<String> getRecentDay(String type) {
        Integer limit = 0;
        Date date = new Date(System.currentTimeMillis());

        List<String> recentDateList = new ArrayList<>();

        if ("Today".equals(type)) {
            String nowDate = TimeUtil.getDateString(date);
            recentDateList.add(nowDate);
        } else if ("Month".equals(type)) {
            String nowDate = TimeUtil.getDateString(date);
            Date firstDate = TimeUtil.getMinMonthDate(nowDate);
            String firstDateString = TimeUtil.getDateString(firstDate);
            while (!firstDateString.equals(nowDate)) {
                recentDateList.add(firstDateString);
                limit++;
                firstDateString = TimeUtil.getBeforDateByNumber(firstDate, limit);
            }
        } else {
            if ("Seven_DAY".equals(type)) {
                limit = 7;
            } else if ("Fifteen_DAT".equals(type)) {
                limit = 15;
            } else if ("Yesterday".equals(type)) {
                limit = 1;
            }
            for (int amount = -limit; amount < 0; amount++) {
                recentDateList.add(TimeUtil.getBeforDateByNumber(date, amount));
            }
        }
        return recentDateList;
    }
}
