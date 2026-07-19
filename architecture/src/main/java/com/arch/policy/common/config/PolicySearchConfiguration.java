package com.arch.policy.common.config;

import com.arch.policy.search.application.AsyncSearchCoordinator;
import com.arch.policy.search.application.LocalSearchWaiters;
import com.arch.policy.search.application.PolicyIncrementalUpdater;
import com.arch.policy.search.application.PolicyStartupRunner;
import com.arch.policy.search.application.SearchStateStore;
import com.arch.policy.search.application.SupplierCallbackService;
import com.arch.policy.search.application.SupplierTaskDispatcher;
import com.arch.policy.search.domain.snapshot.ActiveSnapshotRegistry;
import com.arch.policy.search.domain.snapshot.DefaultSnapshotValidator;
import com.arch.policy.search.domain.snapshot.FileSystemSnapshotDirectory;
import com.arch.policy.search.domain.snapshot.PolicySnapshotService;
import com.arch.policy.search.domain.snapshot.SnapshotBuilder;
import com.arch.policy.search.domain.snapshot.SnapshotPorts.FullPolicyLoader;
import com.arch.policy.search.domain.snapshot.SnapshotPorts.IncrementalReplayer;
import com.arch.policy.search.infrastructure.demo.DemoSupplierTaskDispatcher;
import com.arch.policy.search.infrastructure.kafka.KafkaPolicyChangeListener;
import com.arch.policy.search.infrastructure.redis.RedisSearchStateStore;
import com.arch.policy.search.infrastructure.redis.SearchFinishedSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.file.Paths;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class PolicySearchConfiguration {
    @Bean public ActiveSnapshotRegistry activeSnapshotRegistry() { return new ActiveSnapshotRegistry(); }

    @Bean public PolicyIncrementalUpdater policyIncrementalUpdater(ActiveSnapshotRegistry registry) {
        return new PolicyIncrementalUpdater(registry);
    }

    @Bean(destroyMethod = "shutdown") public Executor policySearchExecutor() {
        return Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    @Bean public ObjectMapper objectMapper() { return new ObjectMapper(); }

    @Bean public SearchStateStore searchStateStore(StringRedisTemplate redis) {
        return new RedisSearchStateStore(redis);
    }

    @Bean public LocalSearchWaiters localSearchWaiters() { return new LocalSearchWaiters(); }

    @Bean public SupplierCallbackService supplierCallbackService(
            SearchStateStore store, ObjectMapper mapper,
            @Value("${policy.search.redis-ttl-seconds:60}") long ttlSeconds) {
        return new SupplierCallbackService(store, mapper, ttlSeconds);
    }

    @Bean(destroyMethod = "shutdown") public ScheduledExecutorService demoSupplierExecutor() {
        return Executors.newScheduledThreadPool(4);
    }

    @Bean public SupplierTaskDispatcher supplierTaskDispatcher(
            ScheduledExecutorService demoSupplierExecutor, SupplierCallbackService callbackService) {
        return new DemoSupplierTaskDispatcher(demoSupplierExecutor, callbackService);
    }

    @Bean public AsyncSearchCoordinator asyncSearchCoordinator(
            SearchStateStore store, SupplierTaskDispatcher dispatcher, LocalSearchWaiters waiters,
            ObjectMapper mapper, @Value("${policy.search.redis-ttl-seconds:60}") long ttlSeconds) {
        return new AsyncSearchCoordinator(store, dispatcher, waiters, mapper, ttlSeconds);
    }

    @Bean public RedisMessageListenerContainer searchFinishedListener(
            RedisConnectionFactory connectionFactory, LocalSearchWaiters waiters) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(new SearchFinishedSubscriber(waiters),
                new ChannelTopic(RedisSearchStateStore.FINISHED_CHANNEL));
        return container;
    }

    @Bean public KafkaPolicyChangeListener kafkaPolicyChangeListener(ObjectMapper mapper,
                                                                      PolicyIncrementalUpdater updater) {
        return new KafkaPolicyChangeListener(mapper, updater);
    }

    @Bean public SnapshotBuilder snapshotBuilder(FullPolicyLoader fullLoader,
                                                  IncrementalReplayer replayer,
                                                  @Value("${policy.snapshot.directory:./data/policy-snapshots}") String directory) {
        return new SnapshotBuilder(fullLoader, replayer, new DefaultSnapshotValidator(),
                new FileSystemSnapshotDirectory(Paths.get(directory)));
    }

    @Bean(destroyMethod = "close") public PolicySnapshotService policySnapshotService(
            SnapshotBuilder builder, ActiveSnapshotRegistry registry,
            @Value("${policy.snapshot.retry-delay-ms:5000}") long retryDelayMillis) {
        return new PolicySnapshotService(builder, registry,
                Executors.newSingleThreadScheduledExecutor(), retryDelayMillis);
    }

    @Bean public PolicyStartupRunner policyStartupRunner(
            PolicySnapshotService service,
            @Value("${policy.snapshot.initial-version:startup}") String initialVersion) {
        return new PolicyStartupRunner(service, initialVersion);
    }
}
