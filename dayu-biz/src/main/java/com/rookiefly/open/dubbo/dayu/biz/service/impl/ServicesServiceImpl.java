package com.rookiefly.open.dubbo.dayu.biz.service.impl;

import com.rookiefly.open.dubbo.dayu.biz.service.DubboMonitorService;
import com.rookiefly.open.dubbo.dayu.biz.service.RegistryContainer;
import com.rookiefly.open.dubbo.dayu.biz.service.ServicesService;
import com.rookiefly.open.dubbo.dayu.common.constants.MonitorConstants;
import com.rookiefly.open.dubbo.dayu.common.tools.NetTools;
import com.rookiefly.open.dubbo.dayu.common.tools.TimeUtil;
import com.rookiefly.open.dubbo.dayu.model.bo.HostBO;
import com.rookiefly.open.dubbo.dayu.model.bo.ServiceBO;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServicesServiceImpl implements ServicesService {

    @Resource
    private RegistryContainer registryContainer;

    @Override
    public Set<String> getAllServicesString() {
        Set<String> resultServices = new ConcurrentHashSet<>();

        Map<String, Map<String, Set<URL>>> registry = registryContainer.getRegistryCache();

        Map<String, Set<URL>> providersServices = registry.get(RegistryConstants.PROVIDERS_CATEGORY);
        Map<String, Set<URL>> consumersServices = registry.get(RegistryConstants.CONSUMERS_CATEGORY);
        for (Map.Entry<String, Set<URL>> serviceEntry : providersServices.entrySet()) {
            String service = serviceEntry.getKey();
            resultServices.add(service);
        }
        for (Map.Entry<String, Set<URL>> serviceEntry : consumersServices.entrySet()) {
            String service = serviceEntry.getKey();
            resultServices.add(service);
        }

        return resultServices;
    }

    @Override
    public Map<String, Set<String>> getServiceProviders() {
        Map<String, Set<String>> serviceProviders = new ConcurrentHashMap<>();

        Map<String, Map<String, Set<URL>>> registry = registryContainer.getRegistryCache();
        Map<String, Set<URL>> providerServices = registry.get(RegistryConstants.PROVIDERS_CATEGORY);

        for (Map.Entry<String, Set<URL>> serviceEntry : providerServices.entrySet()) {
            String service = serviceEntry.getKey();
            Set<String> applications = serviceProviders.get(service);
            if (null == applications) {
                applications = new ConcurrentHashSet<>();
                serviceProviders.put(service, applications);
            }
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                String application = url.getParameter(CommonConstants.APPLICATION_KEY);
                if (null != application) {
                    applications.add(application);
                }
            }
        }
        return serviceProviders;
    }

    @Override
    public Map<String, Set<String>> getServiceConsumers() {
        Map<String, Set<String>> serviceConsumers = new ConcurrentHashMap<>();

        Map<String, Map<String, Set<URL>>> registry = registryContainer.getRegistryCache();
        Map<String, Set<URL>> consumeServices = registry.get(RegistryConstants.CONSUMERS_CATEGORY);

        for (Map.Entry<String, Set<URL>> serviceEntry : consumeServices.entrySet()) {
            String service = serviceEntry.getKey();
            Set<String> applications = serviceConsumers.get(service);
            if (null == applications) {
                applications = new ConcurrentHashSet<>();
                serviceConsumers.put(service, applications);
            }
            Set<URL> urls = serviceEntry.getValue();
            for (URL url : urls) {
                String application = url.getParameter(CommonConstants.APPLICATION_KEY);
                if (null != application) {
                    applications.add(application);
                }
            }
        }
        return serviceConsumers;
    }

    @Override
    public Map<String, ServiceBO> getServiceBOMap() {
        Map<String, ServiceBO> serviceBOMap = new HashMap<>();

        Map<String, Map<String, Set<URL>>> registry = registryContainer.getRegistryCache();

        Map<String, Set<URL>> providersServices = registry.get(RegistryConstants.PROVIDERS_CATEGORY);
        Map<String, Set<URL>> consumersServices = registry.get(RegistryConstants.CONSUMERS_CATEGORY);
        Map<String, Set<URL>> forbidServices = registry.get(RegistryConstants.CONFIGURATORS_CATEGORY);

        //测试环境url
        Set<String> testUrlSet = new HashSet<>();
        for (Map.Entry<String, String> entry : MonitorConstants.ECS_TEST_MAP.entrySet()) {
            testUrlSet.add(entry.getKey());
            testUrlSet.add(entry.getValue());
        }
        //所有服务器url,除测试环境外
        Set<String> onlineUrlSet = new HashSet<>();
        for (Map.Entry<String, String> entry : MonitorConstants.ECS_MAP.entrySet()) {
            String url = entry.getKey();
            if (!testUrlSet.contains(url)) {
                onlineUrlSet.add(url);
            }
        }

        for (Map.Entry<String, Set<URL>> serviceEntry : providersServices.entrySet()) {
            String service = serviceEntry.getKey();
            Set<URL> urlSet = serviceEntry.getValue();

            ServiceBO serviceBO = serviceBOMap.get(service);
            if (null == serviceBO) {
                serviceBO = new ServiceBO();
                serviceBO.setServiceName(service);
            }
            String finalTime = "";
            for (URL url : urlSet) {
                //是否被禁止,禁止则不出现
                Set<URL> forbidSet = forbidServices.get(url.getServiceInterface());
                if (null != forbidSet && !forbidSet.isEmpty() && NetTools.compareIsOverride(url, forbidSet)) {
                    continue;
                }
                String application = url.getParameter(CommonConstants.APPLICATION_KEY);
                String organization = url.getParameter(MonitorConstants.ORGANICATION);
                String owner = url.getParameter(MonitorConstants.OWNER);
                if (StringUtils.isEmpty(serviceBO.getOrganization())) {
                    serviceBO.setOrganization(organization == null ? "" : organization);
                }
                if (StringUtils.isEmpty(serviceBO.getOwner())) {
                    serviceBO.setOwner(owner == null ? "" : owner);
                }

                //method set
                serviceBOSetMethods(serviceBO, url);
                //owner app set
                Set<String> ownerApp = serviceBO.getOwnerApp();
                if (null == ownerApp) {
                    ownerApp = new HashSet<>();
                }
                ownerApp.add(application);
                serviceBO.setOwnerApp(ownerApp);
                //本地起了测试或线上，测试起了线上
                String host = url.getHost();
                String hostTime = DubboMonitorService.getServiceConsumerTime(service, host);
                if (hostTime != null) {
                    //时间处理
                    if (finalTime.equals("")) {
                        finalTime = hostTime;
                    } else {
                        try {
                            //比较时间先后顺序，取靠后的时间
                            Boolean compareResult = TimeUtil.compareTime(hostTime, finalTime);
                            if (compareResult) {
                                finalTime = hostTime;
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (service.endsWith("1.0.0") && !onlineUrlSet.contains(host)) {
                    serviceBO.setIsHostWrong(true);
                }
                if (service.endsWith("1.0.0.daily") && !testUrlSet.contains(host)) {
                    serviceBO.setIsHostWrong(true);
                }
            }

            serviceBO.setFinalConsumerTime(finalTime);
            serviceBOMap.put(service, serviceBO);
        }

        for (Map.Entry<String, Set<URL>> serviceEntry : consumersServices.entrySet()) {
            String service = serviceEntry.getKey();
            Set<URL> urlSet = serviceEntry.getValue();
            ServiceBO serviceBO = serviceBOMap.get(service);
            if (null == serviceBO) {
                serviceBO = new ServiceBO();
                serviceBO.setServiceName(service);
            }
            for (URL url : urlSet) {
                String applicationName = url.getParameter(CommonConstants.APPLICATION_KEY);
                Set<String> usedSet = serviceBO.getUsedApp();
                if (usedSet == null) {
                    usedSet = new HashSet<>();
                }
                usedSet.add(applicationName);
                serviceBO.setUsedApp(usedSet);
            }
            serviceBOMap.put(service, serviceBO);
        }

        return serviceBOMap;
    }

    // 保存method，若method不一致，则保存多条----保存methods所在的host
    private void serviceBOSetMethods(ServiceBO serviceBO, URL url) {
        String methods = url.getParameter(CommonConstants.METHODS_KEY);

        Set<String> methodSet = serviceBO.getMethods();
        if (null == methodSet) {
            methodSet = new HashSet<>();
            methodSet.add(methods);
            serviceBO.setMethods(methodSet);
        } else {
            String oldMethods = methodSet.iterator().next();
            List<String> oldList = Arrays.asList(oldMethods.split(","));
            List<String> nowList = Arrays.asList(methods.split(","));
            boolean isWrong = false;
            if (oldList.size() != nowList.size()) {
                isWrong = true;
            }
            for (String oneMethod : nowList) {
                if (!oldList.contains(oneMethod)) {
                    // 存在不同方法
                    isWrong = true;
                    break;
                }
            }
            if (isWrong) {
                serviceBO.setIsWrong(true);
                methodSet.add(methods);
                serviceBO.setMethods(methodSet);
            } else {
                methods = oldMethods;
            }
        }

        // 添加host 到同一个method上
        Map<String, Set<HostBO>> methodsHost = serviceBO.getMethodsHost();
        if (null == methodsHost) {
            methodsHost = new HashMap<>();
        }

        Set<HostBO> hostSet = methodsHost.get(methods);
        if (null == hostSet) {
            hostSet = new HashSet<>();
        }

        hostSet.add(new HostBO(url.getHost(), String.valueOf(url.getPort())));
        methodsHost.put(methods, hostSet);
        serviceBO.setMethodsHost(methodsHost);
    }
}