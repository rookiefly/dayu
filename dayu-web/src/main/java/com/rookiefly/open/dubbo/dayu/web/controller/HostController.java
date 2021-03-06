package com.rookiefly.open.dubbo.dayu.web.controller;

import com.rookiefly.open.dubbo.dayu.biz.service.HostService;
import com.rookiefly.open.dubbo.dayu.model.vo.ResultVO;
import com.rookiefly.open.dubbo.dayu.model.bo.HostBO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/monitor/hosts")
public class HostController {

    @Resource
    private HostService hostService;

    @RequestMapping(value = "main", method = RequestMethod.GET)
    public ModelAndView main() {
        return new ModelAndView("/host/hostIndex");
    }

    @RequestMapping(value = "/getAllHostPage", method = RequestMethod.GET)
    @ResponseBody
    public ResultVO getAllHostPage() {
        try {
            Map<String, Object> map = new HashMap<>();

            Map<String, HostBO> hostMap = hostService.getHostBOMap();

            List<String> hostList = new ArrayList<>(hostMap.keySet());
            Collections.sort(hostList);
            map.put("sum", hostList.size());
            map.put("hostMap", hostMap);
            map.put("hostList", hostList);
            return ResultVO.wrapSuccessfulResult(map);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultVO.wrapErrorResult(e.getMessage());
        }
    }
}
