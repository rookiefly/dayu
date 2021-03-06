package com.rookiefly.open.dubbo.dayu.web.controller;

import com.rookiefly.open.dubbo.dayu.biz.service.AppChangeService;
import com.rookiefly.open.dubbo.dayu.biz.service.ApplicationService;
import com.rookiefly.open.dubbo.dayu.biz.service.HostService;
import com.rookiefly.open.dubbo.dayu.biz.service.ServicesService;
import com.rookiefly.open.dubbo.dayu.common.tools.TimeUtil;
import com.rookiefly.open.dubbo.dayu.common.constants.MonitorConstants;
import com.rookiefly.open.dubbo.dayu.model.vo.ResultVO;
import com.rookiefly.open.dubbo.dayu.model.bo.ApplicationChangeBO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/monitor/dash")
public class IndexController {
    @Resource
    private ApplicationService applicationService;

    @Resource
    private ServicesService servicesService;

    @Resource
    private HostService hostService;

    @Resource
    private AppChangeService appChangeService;

    @RequestMapping(value = "main", method = RequestMethod.GET)
    public ModelAndView main(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String name = (String) session.getAttribute(MonitorConstants.SESSION_USER_NAME);
        if (name == null) {
            name = "rookiefly";
        }

        ModelAndView modelAndView = new ModelAndView("/main");
        modelAndView.addObject("name", name);
        return modelAndView;
    }

    @RequestMapping(value = "index", method = RequestMethod.GET)
    public ModelAndView index() {
        ModelAndView modelAndView = new ModelAndView("/dashboard/dashboard");

        Integer appSum = applicationService.getAllApplications().size();
        Integer serviceSum = servicesService.getAllServicesString().size();

        Integer hostSum = hostService.getHostBOMap().keySet().size();

        String nowMonth = TimeUtil.getYearMonthString(new Date());

        modelAndView.addObject("appSum", appSum);
        modelAndView.addObject("serviceSum", serviceSum);
        modelAndView.addObject("hostSum", hostSum);
        modelAndView.addObject("nowMonth", nowMonth);

        //最近修改记录
        List<ApplicationChangeBO> recentInsertList = appChangeService.getRecentInsertList();
        List<ApplicationChangeBO> recentDeleteList = appChangeService.getRecentDeleteList();
        modelAndView.addObject("recentInsertList", recentInsertList);
        modelAndView.addObject("recentDeleteList", recentDeleteList);

        return modelAndView;
    }

    /**
     * 获得月份和类型相关的数据
     *
     * @param month
     * @param day
     * @param pageIndex
     * @param pageSize
     * @return
     */
    @RequestMapping(value = "/getMonthChangeData", method = RequestMethod.GET)
    @ResponseBody
    public ResultVO getMonthChangeData(String month, String day, Integer pageIndex, Integer pageSize) {
        Map<String, Object> resultMap = new HashMap<>();

        if (day == null) {
            // 取出第一个日期的数据
            Set<String> daySet = appChangeService.getDaySet(month);
            if (null == daySet || daySet.isEmpty()) {
                return ResultVO.wrapErrorResult("not have record");
            }
            List<String> dayList = new ArrayList<>(daySet);
            Collections.sort(dayList, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return o2.compareTo(o1);
                }
            });
            day = dayList.get(0);
            resultMap.put("day", day);
            resultMap.put("daySet", dayList);
        }
        List<ApplicationChangeBO> resultList = appChangeService.getChangeListByDay(day, pageIndex, pageSize);
        Integer sum = appChangeService.getListSum(day);
        resultMap.put("list", resultList);
        resultMap.put("sum", sum);

        return ResultVO.wrapSuccessfulResult(resultMap);
    }
}
