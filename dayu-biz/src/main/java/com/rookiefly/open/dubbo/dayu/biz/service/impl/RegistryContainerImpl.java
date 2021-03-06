package com.rookiefly.open.dubbo.dayu.biz.service.impl;

import com.rookiefly.open.dubbo.dayu.biz.service.AppChangeService;
import com.rookiefly.open.dubbo.dayu.biz.service.RegistryContainer;
import com.rookiefly.open.dubbo.dayu.common.constants.MonitorConstants;
import com.rookiefly.open.dubbo.dayu.common.tools.NetTools;
import com.rookiefly.open.dubbo.dayu.model.bo.ApplicationChangeBO;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.RegistryConstants;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.registry.Constants;
import org.apache.dubbo.registry.RegistryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RegistryContainerImpl
 * RegistryContainer 的实现类
 */
@Service
public class RegistryContainerImpl implements RegistryContainer {

    /**
     * 自身项目 dubbo注册名
     */
    @Value(value = "${dubbo.application.name}")
    private String myDubboName;

    /**
     * 存关系
     */
    private final Map<String, Map<String, Set<URL>>> registryCache = new ConcurrentHashMap<>();

    /**
     * key:serviceInterface value:Set<service:version>－－用作provider 停止服务时 取消其内容于registryCache中
     */
    private final Map<String, Set<String>> interfaceCache = new ConcurrentHashMap<>();

    /**
     * 变更的app的变化 provider:list<bo> || consumer:list<bo>，每次启动时从redis读取
     */
    private final Map<String, Set<ApplicationChangeBO>> changeAppCaChe = new ConcurrentHashMap<>();

    /**
     * 上次启动时存在redis的数据
     */
    private final Map<String, Set<ApplicationChangeBO>> redisChangeAppCaChe = new ConcurrentHashMap<>();

    /**
     * 判断是否开始监控数据变化
     * time:执行时间
     * startMonitor:是否开始执行
     */
    private final Map<String, Object> finalDataMap = new ConcurrentHashMap<>();
    /**
     * 最后时间戳
     */
    private static final String NOW_TIME_KEY = "now";
    /**
     * 是否初始化完成，可以正常监控
     */
    private static final String IS_START_MONITOR = "isStartMonitor";

    @DubboReference
    private RegistryService registry;

    @Resource
    private AppChangeService appChangeService;


    @Override
    public Map<String, Map<String, Set<URL>>> getRegistryCache() {
        if (!registryCache.containsKey(RegistryConstants.PROVIDERS_CATEGORY)) {
            registryCache.put(RegistryConstants.PROVIDERS_CATEGORY, new ConcurrentHashMap<>());
        }
        if (!registryCache.containsKey(RegistryConstants.CONSUMERS_CATEGORY)) {
            registryCache.put(RegistryConstants.CONSUMERS_CATEGORY, new ConcurrentHashMap<>());
        }
        if (!registryCache.containsKey(RegistryConstants.CONFIGURATORS_CATEGORY)) {
            registryCache.put(RegistryConstants.CONFIGURATORS_CATEGORY, new ConcurrentHashMap<>());
        }

        return Collections.unmodifiableMap(registryCache);
    }

    @Override
    public Date getFinalUpdateTime() {
        Date now = (Date) finalDataMap.get(NOW_TIME_KEY);
        return now;
    }

    /**
     * 初始化changeApp--redis取出,比较后，执行存储
     */
    @Override
    public void initRedisChangeAppCaChe() {
        Map<String, Set<ApplicationChangeBO>> map = appChangeService.getChangeAppCache();
        if (null != map) {
            redisChangeAppCaChe.putAll(map);
        }
        /**
         * 比较此次初始化跟上次的区别
         */
        //insert新增
        for (Map.Entry<String, Set<ApplicationChangeBO>> nowEntry : changeAppCaChe.entrySet()) {
            String category = nowEntry.getKey();
            Set<ApplicationChangeBO> nowSet = nowEntry.getValue();
            Set<ApplicationChangeBO> redisSet = redisChangeAppCaChe.get(category);
            if (null == redisSet) {
                redisSet = new ConcurrentHashSet<>();
            }
            for (ApplicationChangeBO newChangeBO : nowSet) {
                if (!redisSet.contains(newChangeBO)) {
                    appChangeService.afterChangeInsertDo(newChangeBO);
                }
            }
        }

        //delete减少
        for (Map.Entry<String, Set<ApplicationChangeBO>> redisEntry : redisChangeAppCaChe.entrySet()) {
            String category = redisEntry.getKey();
            Set<ApplicationChangeBO> redisSet = redisEntry.getValue();
            Set<ApplicationChangeBO> nowSet = changeAppCaChe.get(category);
            if (null == nowSet) {
                nowSet = new ConcurrentHashSet<>();
            }
            for (ApplicationChangeBO redisBo : redisSet) {
                if (!nowSet.contains(redisBo)) {
                    appChangeService.afterChangeDeleteDo(redisBo);
                }
            }
        }

        finalDataMap.put(IS_START_MONITOR, true);
    }

    @Override
    public void start() {
        URL subscribeUrl = new URL(Constants.ADMIN_PROTOCOL, NetUtils.getLocalHost(), 0, "",
                CommonConstants.INTERFACE_KEY, CommonConstants.ANY_VALUE,
                CommonConstants.GROUP_KEY, CommonConstants.ANY_VALUE,
                CommonConstants.VERSION_KEY, CommonConstants.ANY_VALUE,
                CommonConstants.CLASSIFIER_KEY, CommonConstants.ANY_VALUE,
                RegistryConstants.CATEGORY_KEY, RegistryConstants.PROVIDERS_CATEGORY + ","
                + RegistryConstants.CONSUMERS_CATEGORY + ","
                + RegistryConstants.CONFIGURATORS_CATEGORY,
                CommonConstants.CHECK_KEY, String.valueOf(false));

        finalDataMap.put(IS_START_MONITOR, false);

        // 订阅符合条件的已注册数据，当有注册数据变更时自动推送.
        registry.subscribe(subscribeUrl, urls -> {
            if (urls == null || urls.size() == 0) {
                return;
            }

            // 组合新数据
            final Map<String, Map<String, Set<URL>>> categories = new ConcurrentHashMap<>();
            //此批的提供者 interface:Service
            final Map<String, Set<String>> interfaces = new ConcurrentHashMap<>();

            Date now = new Date();
            //实际逻辑
            for (URL url : urls) {
                //逻辑处理
                String application = url.getParameter(CommonConstants.APPLICATION_KEY);
                if (myDubboName.equals(application)) {
                    continue;
                }
                String category = url.getParameter(RegistryConstants.CATEGORY_KEY, RegistryConstants.DEFAULT_CATEGORY);
                String protocol = url.getProtocol();

                // 移除此数据:涉及provider、consumer的减少／进行数据通知+禁用数据的更改
                if (RegistryConstants.EMPTY_PROTOCOL.equals(protocol)) {
                    urlEmptyDo(category, url);
                    continue;
                }

                Map<String, Set<URL>> services = categories.get(category);
                if (services == null) {
                    services = new ConcurrentHashMap<>();
                    categories.put(category, services);
                }
                String service = url.getServiceInterface();
                if ("org.apache.dubbo.monitor.MonitorService".equals(service)) {
                    continue;
                }
                Set<URL> ids = services.get(service);
                if (ids == null) {
                    ids = new ConcurrentHashSet<>();
                    services.put(service, ids);
                }
                ids.add(url);

                // interface : service
                if (RegistryConstants.PROVIDERS_CATEGORY.equals(category)) {
                    String serviceInterface = url.getServiceInterface();
                    Set<String> interfaceServices = interfaces.get(serviceInterface);
                    if (interfaceServices == null) {
                        interfaceServices = new ConcurrentHashSet<>();
                        interfaces.put(serviceInterface, interfaceServices);
                    }
                    interfaceServices.add(service);
                }
            }
            if (interfaces.size() != 0) {
                // 提供者，批量的interface，涉及provider的减少
                isProviderReduce(interfaces);
            }

            if (categories.size() != 0) {
                //涉及consumer新增、减少；provider新增
                categoryServiceChange(categories);
            }

            finalDataMap.put(NOW_TIME_KEY, now);
        });
    }

    @Override
    public void restart() {
        registryCache.clear();
        start();
    }

    @Override
    @PreDestroy
    public void stop() {
    }

    /**
     * 涉及consumer新增、减少；provider新增
     *
     * @param categories
     */
    private void categoryServiceChange(final Map<String, Map<String, Set<URL>>> categories) {
        boolean appCacheChange = false;
        for (Map.Entry<String, Map<String, Set<URL>>> categoryEntry : categories.entrySet()) {
            String category = categoryEntry.getKey();
            Map<String, Set<URL>> services = registryCache.get(category);
            if (services == null) {
                services = new ConcurrentHashMap<>();
                registryCache.put(category, services);
            }
            Map<String, Set<URL>> NewServices = categoryEntry.getValue();
            if (compareOldAndNewServices(services, NewServices)) {
                appCacheChange = true;
            }
            services.putAll(NewServices);
        }
        //初始化完成方可对比处理数据
        Boolean startMonitor = (Boolean) finalDataMap.get(IS_START_MONITOR);

        //appCache发生变化
        if (appCacheChange && startMonitor) {
            saveChangeAppCaChe();
        }
    }

    /**
     * 涉及consumer新增、减少；provider新增-- 更改保存到列表和 redis中
     *
     * @param oldServices
     * @param newServices
     * @return
     */
    private boolean compareOldAndNewServices(Map<String, Set<URL>> oldServices, Map<String, Set<URL>> newServices) {
        boolean appCacheChange = false;
        for (Map.Entry<String, Set<URL>> oldEntry : oldServices.entrySet()) {
            String service = oldEntry.getKey();
            Set<URL> oldUrlSet = oldEntry.getValue();
            if (!newServices.containsKey(service)) {
                continue;
            }

            Set<URL> newUrlSet = newServices.get(service);

            //减少
            for (URL url : oldUrlSet) {
                if (!newUrlSet.contains(url)) {
                    removeFromChangeAppCaChe(url);
                    appCacheChange = true;
                }
            }

            // 新增
            for (URL url : newUrlSet) {
                if (!oldUrlSet.contains(url)) {
                    addToChangeAppCaChe(url);
                    appCacheChange = true;
                }
            }
        }
        return appCacheChange;
    }

    /**
     * url以empty开头的url处理，移除此数据:涉及provider、consumer的减少
     *
     * @param category
     * @param url
     */
    private void urlEmptyDo(String category, URL url) {
        boolean appCacheChange = false;

        Map<String, Set<URL>> services = registryCache.get(category);
        if (services != null) {
            String group = url.getParameter(CommonConstants.GROUP_KEY);
            String version = url.getParameter(CommonConstants.VERSION_KEY);
            // 注意：empty协议的group和version为*
            if (!CommonConstants.ANY_VALUE.equals(group) && !CommonConstants.ANY_VALUE.equals(version)) {
                String service = url.getServiceInterface();

                Set<URL> urlSet = services.get(service);
                for (URL removeUrl : urlSet) {
                    // 从 changeAppCache 中移除数据
                    appCacheChange = true;
                    removeFromChangeAppCaChe(removeUrl);
                }
                services.remove(service);

            } else {
                String urlService = url.getServiceInterface();
                for (Map.Entry<String, Set<URL>> serviceEntry : services.entrySet()) {
                    String service = serviceEntry.getKey();
                    if (NetTools.getInterface(service).equals(urlService)
                            && (CommonConstants.ANY_VALUE.equals(group) || StringUtils.isEquals(group, NetTools.getGroup(service)))
                            && (CommonConstants.ANY_VALUE.equals(version) || StringUtils.isEquals(version, NetTools.getVersion(service)))) {

                        Set<URL> urlSet = serviceEntry.getValue();
                        for (URL removeUrl : urlSet) {
                            appCacheChange = true;
                            removeFromChangeAppCaChe(removeUrl);
                        }
                        services.remove(service);
                    }
                }
            }
            //初始化完成方可对比处理数据
            Boolean startMonitor = (Boolean) finalDataMap.get(IS_START_MONITOR);

            //appCache发生变化
            if (appCacheChange && startMonitor) {
                saveChangeAppCaChe();
            }
        }
    }

    /**
     * 提供者，批量的interface，涉及providers的减少+禁用数据的更改
     *
     * @param interfaces
     */
    private void isProviderReduce(final Map<String, Set<String>> interfaces) {
        for (Map.Entry<String, Set<String>> interfaceServices : interfaces.entrySet()) {
            String interfaceName = interfaceServices.getKey();
            Set<String> interfaceServicesCache = interfaceCache.get(interfaceName);
            if (null == interfaceServicesCache) {
                interfaceServicesCache = new ConcurrentHashSet<>();
                interfaceCache.put(interfaceName, interfaceServicesCache);
            } else {
                Set<String> interfaceServicesNow = interfaceServices.getValue();
                // 减少的剔除掉，新增的增加
                for (String service : interfaceServicesCache) {
                    if (!interfaceServicesNow.contains(service)) {
                        Map<String, Set<URL>> services = registryCache.get(RegistryConstants.PROVIDERS_CATEGORY);

                        services.remove(service);
                    }
                }
                interfaceCache.put(interfaceName, interfaceServicesNow);
            }
        }
    }

    /**
     * 从appChange 中移除
     *
     * @param url
     */
    private void removeFromChangeAppCaChe(URL url) {
        String host = url.getHost();
        Integer portInt = url.getPort();
        String category = url.getParameter(RegistryConstants.CATEGORY_KEY, RegistryConstants.DEFAULT_CATEGORY);
        String port = String.valueOf(portInt);

        String application = url.getParameter(CommonConstants.APPLICATION_KEY);
        String organization = url.getParameter(MonitorConstants.ORGANICATION);

        if (organization == null) {
            organization = "";
        }

        ApplicationChangeBO applicationChangeBO = new ApplicationChangeBO(host, port, application, category, organization);

        Set<ApplicationChangeBO> oldChangeSet = changeAppCaChe.get(category);
        if (oldChangeSet != null && oldChangeSet.contains(applicationChangeBO)) {
            oldChangeSet.remove(applicationChangeBO);
            //初始化完成方可对比处理数据
            Boolean startMonitor = (Boolean) finalDataMap.get(IS_START_MONITOR);

            if (startMonitor != null && startMonitor) {
                appChangeService.afterChangeDeleteDo(applicationChangeBO);
            }
        }
    }

    /**
     * 从appChange 中新增
     *
     * @param url
     */
    private void addToChangeAppCaChe(URL url) {
        String host = url.getHost();
        Integer portInt = url.getPort();
        String category = url.getParameter(RegistryConstants.CATEGORY_KEY, RegistryConstants.DEFAULT_CATEGORY);
        String port = String.valueOf(portInt);

        String application = url.getParameter(CommonConstants.APPLICATION_KEY);
        String organization = url.getParameter(MonitorConstants.ORGANICATION);

        if (organization == null) {
            organization = "";
        }

        ApplicationChangeBO applicationChangeBO = new ApplicationChangeBO(host, port, application, category, organization);

        Set<ApplicationChangeBO> oldChangeSet = changeAppCaChe.get(category);
        if (null == oldChangeSet) {
            oldChangeSet = new ConcurrentHashSet<>();
            changeAppCaChe.put(category, oldChangeSet);
        }

        //老中没有的添加
        if (!oldChangeSet.contains(applicationChangeBO)) {

            oldChangeSet.add(applicationChangeBO);

            //初始化完成方可对比处理数据
            Boolean startMonitor = (Boolean) finalDataMap.get(IS_START_MONITOR);
            if (startMonitor != null && startMonitor) {
                appChangeService.afterChangeInsertDo(applicationChangeBO);
            }
        }
    }

    /**
     * changeApp--redis存入
     */
    private void saveChangeAppCaChe() {
        Map<String, Set<ApplicationChangeBO>> map = new ConcurrentHashMap<>();
        map.putAll(changeAppCaChe);
        appChangeService.saveChangeAppCache(map);
    }
}