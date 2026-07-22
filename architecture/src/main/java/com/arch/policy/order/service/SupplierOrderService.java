package com.arch.policy.order.service;

import com.arch.policy.order.model.OrderRequest;
import com.arch.policy.order.model.ThirdOrderResult;
import com.arch.policy.order.model.ThirdOrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 三方订单适配器伪实现；tradeOrderSerialNo 是三方请求的稳定幂等号。 */
@Service
public class SupplierOrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupplierOrderService.class);

    public ThirdOrderResult createOrder(String tradeOrderSerialNo, OrderRequest request) {
        LOGGER.info("[三方接口] 幂等创建订单, tradeOrderSerialNo={}, supplierId={}",
                tradeOrderSerialNo, request.getSupplierId());
        // 伪代码默认模拟明确成功；真实适配器需要映射 SUCCESS、FAILED、UNKNOWN 三种结果。
        return new ThirdOrderResult("TP" + tradeOrderSerialNo, ThirdOrderStatus.SUCCESS);
    }

    public ThirdOrderResult queryOrder(String tradeOrderSerialNo, String thirdPartyOrderNo,
                                       String supplierId) {
        LOGGER.info("[三方查询接口] 查询最终订单状态, tradeOrderSerialNo={}, thirdPartyOrderNo={}, supplierId={}",
                tradeOrderSerialNo, thirdPartyOrderNo, supplierId);
        // 伪代码默认模拟查询后成功；长期UNKNOWN需要重试，超过阈值转人工处理。
        return new ThirdOrderResult(thirdPartyOrderNo, ThirdOrderStatus.SUCCESS);
    }
}
