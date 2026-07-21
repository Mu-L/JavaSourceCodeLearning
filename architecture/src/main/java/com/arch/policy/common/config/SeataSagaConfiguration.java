package com.arch.policy.common.config;

import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.config.DbStateMachineConfig;
import io.seata.saga.engine.impl.ProcessCtrlStateMachineEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class SeataSagaConfiguration {
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnBean(DataSource.class)
    public ThreadPoolExecutor seataSagaExecutor() {
        return new ThreadPoolExecutor(2, 16, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1000), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    public DbStateMachineConfig dbStateMachineConfig(
            DataSource dataSource, ThreadPoolExecutor seataSagaExecutor,
            @Value("${spring.application.name:architecture}") String applicationId,
            @Value("${book.seata.tx-service-group:book-saga-group}") String txServiceGroup,
            @Value("${book.seata.tenant-id:book}") String tenantId) {
        DbStateMachineConfig config = new DbStateMachineConfig();
        config.setDataSource(dataSource);
        config.setApplicationId(applicationId);
        config.setTxServiceGroup(txServiceGroup);
        config.setDefaultTenantId(tenantId);
        config.setThreadPoolExecutor(seataSagaExecutor);
        config.setAutoRegisterResources(true);
        config.setResources(new String[] { "classpath*:statelang/book_order_creation_saga.json" });
        config.setSagaJsonParser("jackson");
        config.setSagaBranchRegisterEnable(true);
        return config;
    }

    @Bean
    @ConditionalOnBean(DbStateMachineConfig.class)
    @ConditionalOnMissingBean(StateMachineEngine.class)
    public StateMachineEngine stateMachineEngine(DbStateMachineConfig config) {
        ProcessCtrlStateMachineEngine engine = new ProcessCtrlStateMachineEngine();
        engine.setStateMachineConfig(config);
        return engine;
    }
}
