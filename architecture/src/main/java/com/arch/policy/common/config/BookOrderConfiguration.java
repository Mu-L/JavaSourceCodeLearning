package com.arch.policy.common.config;

import com.arch.policy.book.application.BookOrderApplicationService;
import com.arch.policy.book.application.BookOrderStore;
import com.arch.policy.book.application.CompensationTaskService;
import com.arch.policy.book.application.CreateOrderResultCache;
import com.arch.policy.book.application.DirectOrderCreationSaga;
import com.arch.policy.book.application.GdsBookingGateway;
import com.arch.policy.book.application.GdsReconciliationService;
import com.arch.policy.book.application.OrderCreationSaga;
import com.arch.policy.book.application.OrderCreationOutboxPublisher;
import com.arch.policy.book.application.OrderCreationSagaStateServices;
import com.arch.policy.book.application.OrderEventApplicationService;
import com.arch.policy.book.application.OrderWorkflowTaskStore;
import com.arch.policy.book.application.PaymentGateway;
import com.arch.policy.book.application.PostTransitionExecutor;
import com.arch.policy.book.application.PromotionStockGateway;
import com.arch.policy.book.application.RecoveryTaskProcessor;
import com.arch.policy.book.application.RecoveryTaskStore;
import com.arch.policy.book.application.RetryablePostTransitionExecutor;
import com.arch.policy.book.domain.DefaultOrderTransitions;
import com.arch.policy.book.domain.OrderStateMachine;
import com.arch.policy.book.infrastructure.demo.DemoGdsBookingGateway;
import com.arch.policy.book.infrastructure.job.OrderRecoveryScheduler;
import com.arch.policy.book.infrastructure.job.OrderCreationOutboxScheduler;
import com.arch.policy.book.infrastructure.repository.InMemoryBookOrderStore;
import com.arch.policy.book.infrastructure.repository.InMemoryFailedPostActionStore;
import com.arch.policy.book.infrastructure.repository.InMemoryOrderWorkflowTaskStore;
import com.arch.policy.book.infrastructure.repository.InMemoryRecoveryTaskStore;
import com.arch.policy.book.infrastructure.cache.DefaultCreateOrderResultCache;
import com.arch.policy.common.infrastructure.redis.RedissonDistributedLock;
import com.arch.policy.common.infrastructure.redis.RedissonRedisClient;
import com.arch.policy.common.lock.DistributedLock;
import com.arch.policy.common.redis.RedisClient;
import com.arch.policy.book.infrastructure.seata.SeataOrderCreationSaga;
import io.seata.saga.engine.StateMachineEngine;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class BookOrderConfiguration {
    @Bean public OrderStateMachine orderStateMachine() {
        return new OrderStateMachine(DefaultOrderTransitions.definitions());
    }

    @Bean public BookOrderStore bookOrderStore() { return new InMemoryBookOrderStore(); }
    @Bean public DistributedLock distributedLock(RedissonClient redisson) {
        return new RedissonDistributedLock(redisson);
    }
    @Bean public RedisClient redisClient(RedissonClient redisson) {
        return new RedissonRedisClient(redisson);
    }
    @Bean public CreateOrderResultCache createOrderResultCache(RedisClient redisClient) {
        return new DefaultCreateOrderResultCache(redisClient);
    }
    @Bean public RecoveryTaskStore recoveryTaskStore() { return new InMemoryRecoveryTaskStore(); }
    @Bean public OrderWorkflowTaskStore orderWorkflowTaskStore() {
        return new InMemoryOrderWorkflowTaskStore();
    }
    @Bean public InMemoryFailedPostActionStore failedPostActionStore() {
        return new InMemoryFailedPostActionStore();
    }
    @Bean public PostTransitionExecutor postTransitionExecutor(
            InMemoryFailedPostActionStore failedPostActionStore) {
        return new RetryablePostTransitionExecutor(failedPostActionStore);
    }
    @Bean public OrderEventApplicationService orderEventApplicationService(
            BookOrderStore store, OrderStateMachine stateMachine,
            PostTransitionExecutor postTransitionExecutor,
            CompensationTaskService compensationTaskService,
            CreateOrderResultCache resultCache) {
        return new OrderEventApplicationService(store, stateMachine, postTransitionExecutor,
                compensationTaskService, resultCache);
    }
    @Bean public GdsBookingGateway gdsBookingGateway() { return new DemoGdsBookingGateway(); }
    @Bean public PromotionStockGateway promotionStockGateway() {
        return (orderNo, promotionId) -> { };
    }
    @Bean public PaymentGateway paymentGateway() { return (orderNo, paymentNo) -> { }; }
    @Bean public CompensationTaskService compensationTaskService(RecoveryTaskStore store) {
        return new CompensationTaskService(store);
    }
    @Bean(name = "orderCreationSagaStateServices")
    public OrderCreationSagaStateServices orderCreationSagaStateServices(
            BookOrderStore store, GdsBookingGateway gdsGateway,
            OrderEventApplicationService eventService, CompensationTaskService compensationService,
            OrderWorkflowTaskStore workflowTaskStore) {
        return new OrderCreationSagaStateServices(store, gdsGateway, eventService,
                compensationService, workflowTaskStore);
    }
    @Bean public OrderCreationSaga orderCreationSaga(
            ObjectProvider<StateMachineEngine> engineProvider,
            OrderCreationSagaStateServices services,
            @Value("${book.seata.tenant-id:book}") String tenantId) {
        StateMachineEngine engine = engineProvider.getIfAvailable();
        return engine == null ? new DirectOrderCreationSaga(services)
                : new SeataOrderCreationSaga(engine, tenantId);
    }
    @Bean public BookOrderApplicationService bookOrderApplicationService(
            BookOrderStore store, OrderCreationOutboxPublisher outboxPublisher,
            DistributedLock distributedLock,
            CreateOrderResultCache resultCache) {
        return new BookOrderApplicationService(store, outboxPublisher, distributedLock,
                resultCache);
    }
    @Bean public OrderCreationOutboxPublisher orderCreationOutboxPublisher(
            BookOrderStore store, OrderCreationSaga saga) {
        return new OrderCreationOutboxPublisher(store, saga);
    }
    @Bean public OrderCreationOutboxScheduler orderCreationOutboxScheduler(
            OrderCreationOutboxPublisher publisher) {
        return new OrderCreationOutboxScheduler(publisher);
    }
    @Bean public GdsReconciliationService gdsReconciliationService(
            GdsBookingGateway gdsGateway, OrderCreationSagaStateServices sagaStates) {
        return new GdsReconciliationService(gdsGateway, sagaStates);
    }
    @Bean public RecoveryTaskProcessor recoveryTaskProcessor(
            RecoveryTaskStore taskStore, PromotionStockGateway promotionGateway,
            GdsBookingGateway gdsGateway, PaymentGateway paymentGateway,
            GdsReconciliationService reconciliationService) {
        return new RecoveryTaskProcessor(taskStore, promotionGateway, gdsGateway,
                paymentGateway, reconciliationService);
    }
    @Bean public OrderRecoveryScheduler orderRecoveryScheduler(RecoveryTaskProcessor processor) {
        return new OrderRecoveryScheduler(processor);
    }
}
