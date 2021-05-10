package com.rookiefly.open.dubbo.dayu.biz.service;

import com.rookiefly.open.dubbo.dayu.model.bo.ServiceBO;

import java.util.Map;
import java.util.Set;

public interface ServicesService {

    // 所有service
    Set<String> getAllServicesString();

    // service对应的所有provider--app
    Map<String, Set<String>> getServiceProviders();

    //service对应的所有消费者 --app
    Map<String, Set<String>> getServiceConsumers();

    // service名称，service对象
    Map<String, ServiceBO> getServiceBOMap();
}
