package com.rookiefly.open.dubbo.dayu.model.bo;

import lombok.Data;

/**
 * 方法排行榜的bo类
 */
@Data
public class MethodRankBO {

    /**
     * 方法名
     */
    public String methodName;

    /**
     * 函数
     */
    public String serviceName;

    /**
     * 调用次数
     */
    public Integer usedNum;
}
