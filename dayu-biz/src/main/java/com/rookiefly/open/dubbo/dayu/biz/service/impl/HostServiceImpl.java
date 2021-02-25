package com.rookiefly.open.dubbo.dayu.biz.service.impl;

import com.rookiefly.open.dubbo.dayu.biz.service.HostService;
import com.rookiefly.open.dubbo.dayu.biz.service.RegistryContainer;
import com.rookiefly.open.dubbo.dayu.common.constants.MonitorConstants;
import com.rookiefly.open.dubbo.dayu.model.bo.HostBO;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.RegistryConstants;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class HostServiceImpl implements HostService {

    @Resource
    private RegistryContainer registryContainer;

    @Override
    public Set<HostBO> getHostPortByAppName(String appName) {
        Map<String, Map<String, Set<URL>>> registry = registryContainer.getRegistryCache();

        Map<String, Set<URL>> providersServices = registry.get(RegistryConstants.PROVIDERS_CATEGORY);
        Set<HostBO> hostList = new HashSet<>();

        for (Map.Entry<String, Set<URL>> serviceEntry : providersServices.entrySet()) {
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                String application = url.getParameter(CommonConstants.APPLICATION_KEY);
                if (appName.equals(application)) {
                    hostList.add(new HostBO(url.getHost(), String.valueOf(url.getPort())));
                }
            }
        }
        return hostList;
    }

    @Override
    public Set<String> getAppNameByHost(HostBO hostBO) {
        Set<String> AppNameSet = new HashSet<>();

        String port = hostBO.getPort();
        String host = hostBO.getHost();

        Map<String, Map<String, Set<URL>>> registry = registryContainer.getRegistryCache();

        Map<String, Set<URL>> providersServices = registry.get(RegistryConstants.PROVIDERS_CATEGORY);

        for (Map.Entry<String, Set<URL>> serviceEntry : providersServices.entrySet()) {
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                String urlHost = url.getHost();
                String urlPort = String.valueOf(url.getPort());
                if (urlHost.equals(host)) {
                    if (port == null || port.equals(urlPort)) {
                        String application = url.getParameter(CommonConstants.APPLICATION_KEY);
                        AppNameSet.add(application);
                    }
                }
            }
        }
        if (port == null) {
            Map<String, Set<URL>> consumersServices = registry.get(RegistryConstants.CONSUMERS_CATEGORY);
            for (Map.Entry<String, Set<URL>> serviceEntry : consumersServices.entrySet()) {
                Set<URL> urls = serviceEntry.getValue();
                for (URL url : urls) {
                    String urlHost = url.getHost();
                    if (urlHost.equals(host)) {
                        String application = url.getParameter(CommonConstants.APPLICATION_KEY);
                        AppNameSet.add(application);
                    }
                }
            }
        }
        return AppNameSet;
    }

    @Override
    public Set<String> getServiceByHost(HostBO hostBO) {
        Set<String> serviceSet = new HashSet<>();

        String port = hostBO.getPort();
        String host = hostBO.getHost();

        Map<String, Map<String, Set<URL>>> registry = registryContainer.getRegistryCache();

        Map<String, Set<URL>> providersServices = registry.get(RegistryConstants.PROVIDERS_CATEGORY);

        for (Map.Entry<String, Set<URL>> serviceEntry : providersServices.entrySet()) {
            String serviceName = serviceEntry.getKey();
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                String urlHost = url.getHost();
                String urlPort = String.valueOf(url.getPort());
                if (urlHost.equals(host)) {
                    if (port == null || port.equals(urlPort)) {
                        serviceSet.add(serviceName);
                    }
                }
            }
        }
        if (port == null) {
            Map<String, Set<URL>> consumersServices = registry.get(RegistryConstants.CONSUMERS_CATEGORY);
            for (Map.Entry<String, Set<URL>> serviceEntry : consumersServices.entrySet()) {
                String serviceName = serviceEntry.getKey();
                Set<URL> urls = serviceEntry.getValue();
                for (URL url : urls) {
                    String urlHost = url.getHost();
                    if (urlHost.equals(host)) {
                        serviceSet.add(serviceName);
                    }
                }
            }
        }
        return serviceSet;
    }

    @Override
    public Map<String, HostBO> getHostBOMap() {
        Map<String, HostBO> resultMap = new HashMap<>();

        Map<String, Map<String, Set<URL>>> registry = registryContainer.getRegistryCache();

        Map<String, Set<URL>> providersServices = registry.get(RegistryConstants.PROVIDERS_CATEGORY);
        Map<String, Set<URL>> consumersServices = registry.get(RegistryConstants.CONSUMERS_CATEGORY);

//        //存ip：name
//        public static final Map<String,String> ecsMap = new HashMap<>();
//        // 双向map
//        public static final BiMap<String,String> ecsBiMap = HashBiMap.create();
        // 拼提供者
        for (Map.Entry<String, Set<URL>> serviceEntry : providersServices.entrySet()) {
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                String host = url.getHost();
                String port = String.valueOf(url.getPort());

                String application = url.getParameter(CommonConstants.APPLICATION_KEY);

                HostBO hostBO = resultMap.get(host);
                if (null == hostBO) {
                    hostBO = new HostBO(host, port);
                    String hostName = MonitorConstants.ecsMap.get(host);
                    String anotherIp = MonitorConstants.ecsBiMap.get(host);
                    if (null == hostName) {
                        hostName = "未知设备";
                        anotherIp = "";
                    } else {
                        if (anotherIp == null) {
                            anotherIp = MonitorConstants.ecsBiMap.inverse().get(host);
                        }
                    }
                    hostBO.setHostName(hostName);
                    hostBO.setAnotherIp(anotherIp);
                }
                Set<String> providersSet = hostBO.getProviders();
                if (null == providersSet) {
                    providersSet = new HashSet<>();
                }
                providersSet.add(port + ":" + application);
                hostBO.setProviders(providersSet);
                resultMap.put(host, hostBO);
            }
        }

        // 拼消费者
        for (Map.Entry<String, Set<URL>> serviceEntry : consumersServices.entrySet()) {
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                String host = url.getHost();

                String application = url.getParameter(CommonConstants.APPLICATION_KEY);

                HostBO hostBO = resultMap.get(host);
                if (null == hostBO) {
                    hostBO = new HostBO(host, null);
                    String hostName = MonitorConstants.ecsMap.get(host);
                    String anotherIp = MonitorConstants.ecsBiMap.get(host);
                    if (null == hostName) {
                        hostName = "未知设备";
                        anotherIp = "";
                    }
                    hostBO.setHostName(hostName);
                    hostBO.setAnotherIp(anotherIp);
                }
                Set<String> consumersSet = hostBO.getConsumers();
                if (null == consumersSet) {
                    consumersSet = new HashSet<>();
                }
                consumersSet.add(application);
                hostBO.setConsumers(consumersSet);
                resultMap.put(host, hostBO);
            }
        }

        return resultMap;
    }
}
