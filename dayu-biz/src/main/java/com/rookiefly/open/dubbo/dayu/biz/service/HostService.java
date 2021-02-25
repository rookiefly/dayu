package com.rookiefly.open.dubbo.dayu.biz.service;

import com.rookiefly.open.dubbo.dayu.model.bo.HostBO;

import java.util.Map;
import java.util.Set;

public interface HostService {

    //根据app名称获得所有host和port
    Set<HostBO> getHostPortByAppName(String appName);

    //根据host和port获得app
    Set<String> getAppNameByHost(HostBO hostBO);

    //根据host和port获得service
    Set<String> getServiceByHost(HostBO hostBO);


    // host名称-即ip地址，host对象
    Map<String, HostBO> getHostBOMap();
}
