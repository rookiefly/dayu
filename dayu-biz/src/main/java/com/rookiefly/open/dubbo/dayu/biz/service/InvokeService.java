package com.rookiefly.open.dubbo.dayu.biz.service;

import com.rookiefly.open.dubbo.dayu.model.bo.MethodRankBO;

import java.util.List;

public interface InvokeService {

    /**
     * 根据app名称获得前50位方法排行榜
     *
     * @param appName 应用名称
     * @return 按使用次数从大到小的最多50位排行, 缓存23小时
     */
    List<MethodRankBO> getMethodRankByAppName(String appName);


}
