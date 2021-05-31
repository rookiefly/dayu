package com.rookiefly.open.dubbo.dayu.common.constants;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.HashMap;
import java.util.Map;

/**
 * 常量
 */
public class MonitorConstants {

    public static final String OWNER = "owner";

    public static final String ORGANICATION = "organization";

    public static final String SESSION_USER_NAME = "SESSION_USER_NAME";

    public static final String SUCCESS = "success";

    public static final String FAIL = "fail";

    /**
     * 存所有的----ip：name，例如 10.0.0.1：本地服务器
     */
    public static final Map<String, String> ECS_MAP = new HashMap<>();

    /**
     * 双向map---内网 ip:外网 ip
     */
    public static final BiMap<String, String> ECS_BI_MAP = HashBiMap.create();

    /**
     * 测试环境的ip---内网 ip:外网 ip
     */
    public static final Map<String, String> ECS_TEST_MAP = new HashMap<>();

    public static void initEcsMap() {
        //自定义初始化上述map。将ip放置在内存中。
//        ecsMap.put("10.0.0.1","测试服务器");
//
//        ecsBiMap.put("10.0.0.1","192.xxx.x0.xx");
//        ecsBiMap.put("10.0.0.2","192.xxx.x1.xx");
//
//        ecsTestMap.put("10.0.0.2","192.xxx.x1.xx");
    }
}
