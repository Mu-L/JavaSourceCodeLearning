package com.arch.policy.book.application;

import com.arch.policy.book.domain.BookOrder;
import com.arch.policy.common.book.BookOrderRequest;
import com.arch.policy.common.book.BookOrderResponse;
import com.arch.policy.common.lock.DistributedLock;
import com.arch.policy.common.lock.DistributedLockUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.UUID;

public final class BookOrderApplicationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BookOrderApplicationService.class);
    private static final DistributedLock.LockHandle NO_OP_LOCK = () -> { };
    private static final long CREATE_LOCK_WAIT_MILLIS = 300L;
    private static final String CREATE_ORDER_LOCK_KEY_PREFIX = "book:create:lock:";
    private static final long CREATE_RESULT_WAIT_MILLIS = 500L;
    private static final long CREATE_RESULT_TTL_MILLIS = 600_000L;
    private final BookOrderStore store;
    private final OrderCreationOutboxPublisher outboxPublisher;
    private final DistributedLock distributedLock;
    private final CreateOrderResultCache resultCache;

    public BookOrderApplicationService(BookOrderStore store,
                                       OrderCreationOutboxPublisher outboxPublisher,
                                       DistributedLock distributedLock,
                                       CreateOrderResultCache resultCache) {
        this.store = store;
        this.outboxPublisher = outboxPublisher;
        this.distributedLock = distributedLock;
        this.resultCache = resultCache;
    }

    /**
     * 创建订单并在本地事务提交后启动 GDS 占编 Saga。
     *
     * <p>流程边界：</p>
     * <ol>
     *     <li>使用 requestId 查询 Redis 幂等结果缓存，命中则直接返回。</li>
     *     <li>获取 requestId 对应的 Redisson 短锁并进行缓存二次检查。</li>
     *     <li>在同一本地事务中保存 CREATE 状态订单和 Saga 启动 Outbox。</li>
     *     <li>事务提交后发布 Outbox，使用 orderNo 幂等启动 Seata Saga。</li>
     *     <li>Saga 根据 GDS 的 SUCCESS、FAIL、UNKNOWN 结果分别推进至
     *         WAIT_PAY、CREATE_FAIL、VALIDATING。</li>
     *     <li>重新读取订单并返回 Saga 执行后的最新状态。</li>
     * </ol>
     *
     * <p>如果进程在订单提交后、Saga 启动前退出，定时发布器会重新投递未发布的 Outbox；
     * Seata 根据持久化的状态机日志恢复已经启动的 Saga。</p>
     *
     * @param request 下单请求，requestId 是业务幂等键
     * @return 当前最新订单状态；同步 Saga 通常返回 WAIT_PAY、CREATE_FAIL 或 VALIDATING
     * @throws IllegalArgumentException 请求参数不合法时抛出
     */
    public BookOrderResponse createOrder(BookOrderRequest request) {
        validate(request);

        // 锁前只访问 Redis，不查询数据库，避免大量重复请求同时穿透到主库。
        BookOrderResponse cached = resultCache.get(request.getRequestId());
        if (cached != null) return cached;

        DistributedLock.LockHandle lock = acquireCreateOrderLock(request.getRequestId());
        if (lock == null) return responseOfCompetingRequest(request.getRequestId());

        BookOrder order;
        OrderCreationOutboxMessage outboxMessage;
        try {
            // Double check：前一个持锁请求可能已在等待期间完成创建并回填缓存。
            cached = resultCache.get(request.getRequestId());
            if (cached != null) return cached;

            // 只有锁持有者才能访问数据库；用于缓存丢失、过期及 Redis 降级场景。
            BookOrder existing = store.findByRequestId(request.getRequestId());
            if (existing != null) {
                BookOrderResponse existingResponse = response(existing);
                resultCache.put(request.getRequestId(), existingResponse, CREATE_RESULT_TTL_MILLIS);
                return existingResponse;
            }

            // 构造下单对象（模拟下单的内容）
            order = BookOrder.create(newOrderNo(), request.getRequestId(), request.getCustomerId(),
                    request.getProductId(), request.getPromotionId(), request.getQuantity(),
                    request.getAmount());

            // 数据库 request_id 唯一索引是最终幂等防线。
            outboxMessage = new OrderCreationOutboxMessage(order.getOrderNo(),
                    order.getPromotionId());
            LocalCreateResult localResult = store.createOrder(order, outboxMessage);
            if (localResult.isDuplicateRequest()) {
                BookOrderResponse duplicateResponse = response(localResult.getOrder());
                resultCache.put(request.getRequestId(), duplicateResponse, CREATE_RESULT_TTL_MILLIS);
                return duplicateResponse;
            }
            resultCache.put(request.getRequestId(), response(order), CREATE_RESULT_TTL_MILLIS);
        } finally {
            lock.close();
        }

        // Redis 锁已释放；立即发布一次，崩溃遗漏由定时 Outbox 发布器补发。
        try {
            outboxPublisher.publish(outboxMessage);
        } catch (RuntimeException publishFailure) {
            LOGGER.warn("Creation Outbox will be retried, orderNo={}", order.getOrderNo(),
                    publishFailure);
        }

        // 同步 Saga 可能已经改变订单状态，因此返回前重新读取聚合快照。
        BookOrder latest = store.findByOrderNo(order.getOrderNo());
        BookOrderResponse latestResponse = response(latest == null ? order : latest);
        resultCache.put(request.getRequestId(), latestResponse, CREATE_RESULT_TTL_MILLIS);
        return latestResponse;
    }

    private BookOrderResponse responseOfCompetingRequest(String requestId) {
        BookOrderResponse cached = resultCache.await(requestId, CREATE_RESULT_WAIT_MILLIS);
        if (cached != null) return cached;
        throw new IllegalStateException("create order request is being processed: " + requestId);
    }

    private DistributedLock.LockHandle acquireCreateOrderLock(String requestId) {
        String key = CREATE_ORDER_LOCK_KEY_PREFIX + requestId;
        try {
            return distributedLock.tryAcquire(key, CREATE_LOCK_WAIT_MILLIS);
        } catch (DistributedLockUnavailableException unavailable) {
            // 数据库 request_id 唯一索引仍是最终幂等防线，Redis 故障时允许降级。
            LOGGER.warn("Create-order lock unavailable, fall back to database idempotency, key={}",
                    key, unavailable);
            return NO_OP_LOCK;
        }
    }

    private static void validate(BookOrderRequest request) {
        if (request == null) throw new IllegalArgumentException("request is required");
        if (isBlank(request.getRequestId())) throw new IllegalArgumentException("requestId is required");
        if (isBlank(request.getCustomerId())) throw new IllegalArgumentException("customerId is required");
        if (isBlank(request.getProductId())) throw new IllegalArgumentException("productId is required");
        if (request.getQuantity() <= 0) throw new IllegalArgumentException("quantity must be positive");
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
    }

    private static String newOrderNo() {
        return "BO" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }

    private static boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
    private static BookOrderResponse response(BookOrder order) {
        return new BookOrderResponse(order.getOrderNo(), order.getState().name(), order.getVersion());
    }
}
