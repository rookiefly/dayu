package com.rookiefly.open.dubbo.dayu.model.bo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * 变更的app bo 类
 */
@Data
@ToString
public class ApplicationChangeBO implements Serializable {

    private String host;

    private String port;

    private String appName;

    private String time;

    private String category;

    private String organization;

    private String hostString;

    /**
     * 执行的操作；insert／delete
     **/
    private String doType;

    public ApplicationChangeBO() {
    }

    public ApplicationChangeBO(String host, String port, String appName, String category, String organization) {
        this.host = host;
        this.port = port;
        this.appName = appName;
        this.category = category;
        this.organization = organization;
    }

    public String getHostString() {
        if (port == null || "0".equals(port)) {
            return host;
        }
        return host + ":" + port;
    }
}
