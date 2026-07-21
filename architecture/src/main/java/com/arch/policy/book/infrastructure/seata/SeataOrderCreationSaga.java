package com.arch.policy.book.infrastructure.seata;

import com.arch.policy.book.application.OrderCreationSaga;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.statelang.domain.StateMachineInstance;

import java.util.HashMap;
import java.util.Map;

public final class SeataOrderCreationSaga implements OrderCreationSaga {
    public static final String STATE_MACHINE_NAME = "BookOrderCreationSaga";
    private final StateMachineEngine stateMachineEngine;
    private final String tenantId;

    public SeataOrderCreationSaga(StateMachineEngine stateMachineEngine, String tenantId) {
        this.stateMachineEngine = stateMachineEngine;
        this.tenantId = tenantId;
    }

    /**
     * 启动订单创建 Saga：调用 GDS 占编，并根据结果推进订单状态或登记补偿任务。
     */
    @Override public SagaStartResult start(String orderNo, String promotionId) {
        // Saga 状态节点通过上下文读取订单号和促销活动 ID，作为服务方法的入参。
        Map<String, Object> context = new HashMap<String, Object>();
        context.put("orderNo", orderNo);
        // Seata 表达式和服务参数统一使用空字符串，避免 null 在状态机流转中产生歧义。
        context.put("promotionId", promotionId == null ? "" : promotionId);

        // orderNo 作为业务键标识该订单的 Saga 实例，便于恢复任务关联同一笔订单流程。
        StateMachineInstance instance = stateMachineEngine.startWithBusinessKey(
                STATE_MACHINE_NAME, tenantId, orderNo, context);

        // 向上层返回 Saga 实例 ID，以及状态机是否仍处于运行中。
        return new SagaStartResult(instance.getId(), instance.isRunning());
    }
}
