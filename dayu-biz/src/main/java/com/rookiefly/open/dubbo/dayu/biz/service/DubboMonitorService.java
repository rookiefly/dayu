package com.rookiefly.open.dubbo.dayu.biz.service;

import com.rookiefly.open.dubbo.dayu.common.tools.TimeUtil;
import com.rookiefly.open.dubbo.dayu.common.tools.UUIDGenerator;
import com.rookiefly.open.dubbo.dayu.dao.mapper.InvokeDOMapper;
import com.rookiefly.open.dubbo.dayu.dao.redis.manager.InvokeRedisManager;
import com.rookiefly.open.dubbo.dayu.model.bo.HostBO;
import com.rookiefly.open.dubbo.dayu.model.entity.InvokeDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ConfigUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.monitor.MonitorService;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@DubboService(timeout = 60000)
public class DubboMonitorService implements MonitorService {

    @Resource
    private HostService hostService;

    private final BlockingQueue<URL> queue;

    private final BlockingQueue<InvokeDO> saveSqlQueue;

    private static final String POISON_PROTOCOL = "poison";

    private volatile boolean running = true;

    /**
     * 方法最后的消费时间
     */
    private static final Map<String, String> SERVICE_FINAL_TIME_MAP = new ConcurrentHashMap<>();

    /**
     * 定时任务执行器
     */
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(3, new NamedThreadFactory("DubboMonitorTimer", true));

    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(5, new NamedThreadFactory("DubboMonitorThreadPool", true));

    private final ScheduledFuture<?> scheduledFuture;

    @Resource
    private InvokeRedisManager invokeRedisManager;

    @Resource(name = "invokeDOMapper")
    private InvokeDOMapper invokeDOMapper;

    public DubboMonitorService() {
        queue = new LinkedBlockingQueue<>(Integer.parseInt(ConfigUtils.getProperty("dubbo.monitor.queue", "100000")));
        saveSqlQueue = new LinkedBlockingQueue<>(Integer.parseInt(ConfigUtils.getProperty("dubbo.monitor.sql_data", "100000")));

        // 保存数据到redis和数据库
        saveData();
        scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            while (running) {
                try {
                    if (queue.isEmpty()) {
                        break;
                    }
                    saveInvoke(); // 记录统计日志
                } catch (Throwable t) { // 防御性容错
                    log.error("Unexpected error occur at write stat log, cause: " + t.getMessage(), t);
                    try {
                        Thread.sleep(5000); // 失败延迟
                    } catch (Throwable t2) {
                        log.error("sleep then still Throwable");
                        t2.printStackTrace();
                    }
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void saveData() {
        taskExecutor.submit(() -> {
            while (true) {
                if (saveSqlQueue.isEmpty()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                InvokeDO invokeDO = null;
                try {
                    invokeDO = saveSqlQueue.take();
                } catch (InterruptedException e) {
                    log.info("saveSqlQueue error" + e.getMessage(), e);
                }
                if (invokeDO != null) {
                    String hour = TimeUtil.getHourString(new Date());
                    // 缓存放一份
                    invokeRedisManager.saveInvoke(hour, invokeDO);
                    // 持久化放一份
                    invokeDOMapper.insertSelective(invokeDO);
                }
            }
        });

    }

    /**
     * 获得service最后被消费的时间
     */
    public static String getServiceConsumerTime(String serviceName, String provideHost) {
        String key = serviceName + provideHost;
        return SERVICE_FINAL_TIME_MAP.get(key);
    }

    @Override
    public void collect(URL statistics) {
        queue.offer(statistics);
    }

    @Override
    public List<URL> lookup(URL url) {
        return null;
    }

    private void saveInvoke() throws Exception {
        if (queue.isEmpty()) {
            return;
        }
        URL statistics = queue.take();

        if (POISON_PROTOCOL.equals(statistics.getProtocol())) {
            return;
        }
        String timestamp = statistics.getParameter(CommonConstants.TIMESTAMP_KEY);
        Date now;
        if (timestamp == null || timestamp.length() == 0) {
            now = new Date();
        } else if (timestamp.length() == "yyyyMMddHHmmss".length()) {
            now = new SimpleDateFormat("yyyyMMddHHmmss").parse(timestamp);
        } else {
            now = new Date(Long.parseLong(timestamp));
        }

        HostBO hostBO = null;
        InvokeDO dubboInvoke = new InvokeDO();

        dubboInvoke.setUuId(UUIDGenerator.getUUID());
        if (statistics.hasParameter(PROVIDER)) {
            dubboInvoke.setAppType(CONSUMER);
            dubboInvoke.setConsumerHost(statistics.getHost());
            String provider = statistics.getParameter(PROVIDER);
            int i = provider.indexOf(':');
            if (i > 0) {
                String[] providerArray = provider.split(":");
                dubboInvoke.setProviderHost(providerArray[0]);
                dubboInvoke.setProviderPort(providerArray[1]);
                hostBO = new HostBO(providerArray[0], providerArray[1]);
            } else {
                dubboInvoke.setProviderHost(provider);
            }

        } else {
            //不存储提供者记录，暂时无用
            return;
//            dubboInvoke.setAppType(PROVIDER);
//            dubboInvoke.setProviderHost(statistics.getHost());
//            dubboInvoke.setProviderPort(String.valueOf(statistics.getPort()));
//
//            String consumer = statistics.getParameter(CONSUMER);
//            int i = consumer.indexOf(':');
//            if (i > 0) {
//                String[] consumerArray = consumer.split(":");
//                dubboInvoke.setConsumerHost(consumerArray[0]);
//                dubboInvoke.setConsumerPort(consumerArray[1]);
//            }else{
//                dubboInvoke.setConsumerHost(consumer);
//            }
        }
        dubboInvoke.setApplication(statistics.getParameter(APPLICATION, ""));
        dubboInvoke.setService(statistics.getServiceInterface());
        dubboInvoke.setMethod(statistics.getParameter(METHOD));
        dubboInvoke.setInvokeTime(statistics.getParameter(TIMESTAMP, System.currentTimeMillis()));
        dubboInvoke.setSuccess(statistics.getParameter(SUCCESS, 0));
        dubboInvoke.setFailure(statistics.getParameter(FAILURE, 0));
        dubboInvoke.setElapsed(statistics.getParameter(ELAPSED, 0));
        dubboInvoke.setConcurrent(statistics.getParameter(CONCURRENT, 0));
        dubboInvoke.setMaxElapsed(statistics.getParameter(MAX_ELAPSED, 0));
        dubboInvoke.setMaxConcurrent(statistics.getParameter(MAX_CONCURRENT, 0));


        String date = TimeUtil.getDateString(now);
        String hour = TimeUtil.getHourString(now);
        dubboInvoke.setInvokeDate(date);
        dubboInvoke.setInvokeHour(hour);

        if (dubboInvoke.getSuccess() == 0 && dubboInvoke.getFailure() == 0 && dubboInvoke.getElapsed() == 0
                && dubboInvoke.getConcurrent() == 0 && dubboInvoke.getMaxElapsed() == 0 && dubboInvoke.getMaxConcurrent() == 0) {
            return;
        }

        //保存其最后被消费时间
        if (hostBO != null) {
            String time = TimeUtil.getTimeString(now);
            String this_service = statistics.getServiceInterface();
            Set<String> serviceSet = hostService.getServiceByHost(hostBO);
            for (String service : serviceSet) {
                if (service.startsWith(this_service)) {
                    String key = service + hostBO.getHost();
                    SERVICE_FINAL_TIME_MAP.put(key, time);
                    break;
                }
            }
        }

        // 往数据库里面塞数据
        saveSqlQueue.offer(dubboInvoke);
    }

    @PreDestroy
    private void destroy() {
        try {
            running = false;
            scheduledFuture.cancel(true);
//            queue.offer(new URL(POISON_PROTOCOL, NetUtils.LOCALHOST, 0));
        } catch (Throwable t) {
            log.warn(t.getMessage(), t);
        }
    }
}
