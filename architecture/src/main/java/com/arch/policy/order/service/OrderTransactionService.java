package com.arch.policy.order.service;

import com.arch.policy.order.model.OrderRequest;
import com.arch.policy.order.model.OrderStatus;
import com.arch.policy.order.model.TradeOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** 只负责订单库的本地事务，示例使用日志代替真实DB操作。 */
@Service
public class OrderTransactionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderTransactionService.class);

    public TradeOrder findByRequest(String orderSerialNo, String supplierId) {
        LOGGER.info("[订单库] 按 orderSerialNo + supplierId 查询幂等订单, orderSerialNo={}, supplierId={}",
                orderSerialNo, supplierId);
        return null;
    }

    public TradeOrder createOrder(OrderRequest request) {
        String tradeOrderSerialNo = newTradeOrderSerialNo();
        LOGGER.info("[订单库事务-开始] 创建子订单");
        LOGGER.info("[订单库] 插入子订单, orderSerialNo={}, tradeOrderSerialNo={}, supplierId={}, status={}",
                request.getOrderSerialNo(), tradeOrderSerialNo, request.getSupplierId(), OrderStatus.CREATE);
        LOGGER.info("[订单库事务-提交] 子订单提交；提交后由应用服务同步启动Seata Saga");
        return new TradeOrder(request.getOrderSerialNo(), tradeOrderSerialNo,
                request.getSupplierId(), OrderStatus.CREATE);
    }

    public void updateStatus(TradeOrder order, OrderStatus status) {
        order.setStatus(status);
        LOGGER.info("[订单库事务] CAS更新订单状态, tradeOrderSerialNo={}, status={}",
                order.getTradeOrderSerialNo(), status);
    }

    private static String newTradeOrderSerialNo() {
        return "TO" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 20).toUpperCase();
    }
}
