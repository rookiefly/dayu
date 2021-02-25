package com.rookiefly.open.dubbo.dayu.biz.support.processor;

import com.rookiefly.open.dubbo.dayu.model.bo.ApplicationChangeBO;
import org.springframework.stereotype.Service;

/**
 * 应用停止和启动事件捕获后的处理机制
 * 可做邮件通知、电话通知、短信通知等相关应用负责人的代码
 */
@Service
public class NotifyAppChangeProcessor {


    public void stopApp(ApplicationChangeBO applicationChangeBO) {
        //todo
    }

    public void startApp(ApplicationChangeBO applicationChangeBO) {
        //todo
    }
}
