package com.rookiefly.open.dubbo.dayu.web.task;

import com.rookiefly.open.dubbo.dayu.common.tools.TimeUtil;
import com.rookiefly.open.dubbo.dayu.dao.mapper.InvokeDOMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 普通的任务
 */
@Component
@Slf4j
public class CommonTask {

    @Resource
    private InvokeDOMapper invokeDOMapper;

    /**
     * 每天凌晨 00：30分执行
     */
    @Scheduled(cron = "0 30 0 * * ?")
    public void everyDayDo() {
        try {
            /**
             * 每天删除 大于15天的日期的原始数据
             */
            String minDate = TimeUtil.getBeforDateByNumber(new Date(), -15);
            invokeDOMapper.deleteByDate(minDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}