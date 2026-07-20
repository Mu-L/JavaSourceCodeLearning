package com.arch.policy.common.config;

import com.arch.policy.book.application.BookOrderApplicationService;
import com.arch.policy.book.application.BookOrderStore;
import com.arch.policy.book.application.PostTransitionExecutor;
import com.arch.policy.book.application.RetryablePostTransitionExecutor;
import com.arch.policy.book.domain.DefaultOrderTransitions;
import com.arch.policy.book.domain.OrderStateMachine;
import com.arch.policy.book.infrastructure.repository.InMemoryBookOrderStore;
import com.arch.policy.book.infrastructure.repository.InMemoryFailedPostActionStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BookOrderConfiguration {
    @Bean public OrderStateMachine orderStateMachine() {
        return new OrderStateMachine(DefaultOrderTransitions.definitions());
    }

    @Bean public BookOrderStore bookOrderStore() { return new InMemoryBookOrderStore(); }

    @Bean public InMemoryFailedPostActionStore failedPostActionStore() {
        return new InMemoryFailedPostActionStore();
    }

    @Bean public PostTransitionExecutor postTransitionExecutor(
            InMemoryFailedPostActionStore failedPostActionStore) {
        return new RetryablePostTransitionExecutor(failedPostActionStore);
    }

    @Bean public BookOrderApplicationService bookOrderApplicationService(
            BookOrderStore store, OrderStateMachine stateMachine,
            PostTransitionExecutor postTransitionExecutor) {
        return new BookOrderApplicationService(store, stateMachine, postTransitionExecutor);
    }
}
