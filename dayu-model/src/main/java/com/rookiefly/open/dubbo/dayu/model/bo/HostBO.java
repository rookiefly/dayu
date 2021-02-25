package com.rookiefly.open.dubbo.dayu.model.bo;

import lombok.Data;
import lombok.ToString;

import java.util.Set;

@ToString
@Data
public class HostBO {

    private String host;

    private String port;

    private String hostString;

    private Set<String> providers;

    private Set<String> consumers;

    /**
     * 服务名--即dba定义的该ip地址的名称
     */
    private String hostName;

    /**
     * 对应的另外一个ip
     */
    private String anotherIp;

    public HostBO() {
    }

    public HostBO(String host, String port) {
        this.host = host;
        this.port = port;
    }

    public String getHostString() {
        if (port == null) {
            return host;
        }
        return host + ":" + port;
    }
}
