package com.arch.policy.order.service;

import com.arch.policy.order.model.OrderRequest;
import com.arch.policy.order.model.TradeOrder;
import io.seata.saga.engine.StateMachineEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/** 使用 tradeOrderSerialNo 作为业务幂等键启动持久化 Seata Saga。 */
@Component
public final class SeataOrderCreationSaga implements OrderCreationSaga {
    static final String STATE_MACHINE_NAME = "TradeOrderCreationSaga";

    @Resource
    private StateMachineEngine stateMachineEngine;
    @Value("${order.seata.tenant-id:order}")
    private String tenantId;

    @Override
    public void start(TradeOrder order, OrderRequest request) {
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("order", order);
        context.put("request", request);
        stateMachineEngine.startWithBusinessKey(STATE_MACHINE_NAME, tenantId,
                order.getTradeOrderSerialNo(), context);
    }
}
