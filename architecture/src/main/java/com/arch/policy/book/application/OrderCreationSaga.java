package com.arch.policy.book.application;

public interface OrderCreationSaga {

    /** 使用 orderNo 作为业务键幂等启动；重复调用不得重复执行外部业务动作。 */
    SagaStartResult start(String orderNo, String promotionId);

    final class SagaStartResult {
        private final String instanceId;
        private final boolean running;

        public SagaStartResult(String instanceId, boolean running) {
            this.instanceId = instanceId;
            this.running = running;
        }

        public String getInstanceId() { return instanceId; }
        public boolean isRunning() { return running; }
    }
}
