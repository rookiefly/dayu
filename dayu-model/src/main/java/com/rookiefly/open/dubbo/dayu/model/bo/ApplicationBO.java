package com.rookiefly.open.dubbo.dayu.model.bo;

import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * app 的基础bizBean类
 */
@Data
public class ApplicationBO {

    private String applicationName;

    private String owner = "";

    /**
     * 所属团队
     */
    private String organization;

    /**
     * 提供服务的ip列表
     */
    private Set<HostBO> hostList;

    /**
     * Service:online-test-local-wrong 四种类型
     */
    private Map<String, Set<ServiceBO>> serviceMap;

    private Set<String> providersSet;

    private Set<String> consumersSet;

    private Boolean isProvider = false;

    private Boolean isConsumer = false;

    private Integer serviceSum = 0;

    private Integer providerSum = 0;

    private Integer consumerSum = 0;
}
