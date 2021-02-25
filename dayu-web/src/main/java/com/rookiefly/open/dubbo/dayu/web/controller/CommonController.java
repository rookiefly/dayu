package com.rookiefly.open.dubbo.dayu.web.controller;

import com.rookiefly.open.dubbo.dayu.biz.service.RegistryContainer;
import com.rookiefly.open.dubbo.dayu.common.tools.TimeUtil;
import com.rookiefly.open.dubbo.dayu.model.vo.ResultVO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Date;

@Controller
@RequestMapping("/monitor/common")
public class CommonController {

    @Resource
    private RegistryContainer registryContainer;

    @RequestMapping(value = "/getFinalTime", method = RequestMethod.GET)
    @ResponseBody
    public ResultVO getFinalTime() {
        Date finalTime = registryContainer.getFinalUpdateTime();
        String timeString = TimeUtil.getTimeString(finalTime);

        return ResultVO.wrapSuccessfulResult(timeString);
    }
}
