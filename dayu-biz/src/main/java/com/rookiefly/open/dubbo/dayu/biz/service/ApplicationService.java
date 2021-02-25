package com.rookiefly.open.dubbo.dayu.biz.service;

import com.rookiefly.open.dubbo.dayu.model.bo.ApplicationBO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ApplicationService {
    //热数据
    Set<String> getAllApplications();

    //缓存一个小时的数据，不关心即时性，可在里面取,无重复
    List<String> getAllApplicationsCache();

    // app名称，app对象
    Map<String, ApplicationBO> getApplicationsBOMap();
}
