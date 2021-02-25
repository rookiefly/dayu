SET
MODE MySQL; -- for h2 test

CREATE TABLE `dubbo_invoke`
(
    `uu_id`          varchar(255) NOT NULL DEFAULT '' COMMENT 'uuid,唯一主键',
    `application`    varchar(255) NOT NULL DEFAULT '' COMMENT 'application名称',
    `app_type`       varchar(255)          DEFAULT '' COMMENT '本身类型',
    `service`        varchar(255)          DEFAULT '' COMMENT 'service 函数',
    `method`         varchar(255)          DEFAULT '' COMMENT '服务',
    `consumer_host`  varchar(255) NOT NULL DEFAULT '' COMMENT '消费者ip',
    `consumer_port`  varchar(255) NOT NULL DEFAULT '' COMMENT '消费者端口号',
    `provider_host`  varchar(255) NOT NULL DEFAULT '' COMMENT '提供者ip',
    `provider_port`  varchar(255) NOT NULL DEFAULT '' COMMENT '提供者端口号',
    `success`        int(11) unsigned DEFAULT '0' COMMENT '成功次数',
    `failure`        int(11) unsigned DEFAULT '0' COMMENT '失败次数',
    `elapsed`        int(11) unsigned DEFAULT '0' COMMENT '总耗时--ms',
    `concurrent`     int(11) unsigned DEFAULT '0' COMMENT '并发数',
    `max_elapsed`    int(11) unsigned DEFAULT '0' COMMENT '最大耗时',
    `max_concurrent` int(11) unsigned DEFAULT '0' COMMENT '最大并发数',
    `invoke_date`    char(10)              DEFAULT NULL COMMENT '日期:yyyy-MM-dd',
    `invoke_hour`    char(2)               DEFAULT NULL COMMENT '小时：hh 例如——9:11 即09',
    `invoke_time`    bigint(20) DEFAULT NULL COMMENT 'dubbo服务 响应的时间',
    `gmt_create`     datetime              DEFAULT NULL COMMENT '默认创建时间',
    PRIMARY KEY (`uu_id`),
    KEY              `index_service` (`service`),
    KEY              `index_method` (`method`),
    KEY              `index_date` (`invoke_date`),
    KEY              `index_hour` (`invoke_hour`)
) COMMENT='存储持久化数据，保存日期为30天'