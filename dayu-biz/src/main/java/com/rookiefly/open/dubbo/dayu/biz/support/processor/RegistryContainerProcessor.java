package com.rookiefly.open.dubbo.dayu.biz.support.processor;

import com.rookiefly.open.dubbo.dayu.biz.service.RegistryContainer;
import com.rookiefly.open.dubbo.dayu.common.constants.MonitorConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class RegistryContainerProcessor implements ApplicationListener<ContextRefreshedEvent> {

    @Resource
    private RegistryContainer registryContainer;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext().getParent();
        if (applicationContext == null) {
            //需要执行的逻辑代码，当spring容器初始化完成后就会执行该方法。
            log.info("=====RegistryContainer start]");
            registryContainer.start();
            //ip地址的初始化
            MonitorConstants.initEcsMap();
        } else {
            //dubbo的数据初始化后的操作
            log.info("=====initRedisChangeAppCaChe]");
            registryContainer.initRedisChangeAppCaChe();
        }

    }
}
