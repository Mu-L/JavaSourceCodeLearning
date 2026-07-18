package com.arch.policy.kafka;

import com.arch.policy.MessagePosition;
import com.arch.policy.PolicyChange;
import com.arch.policy.PolicyRecord;
import com.arch.policy.application.PolicyIncrementalUpdater;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public final class KafkaPolicyChangeListener {
    private final ObjectMapper objectMapper;
    private final PolicyIncrementalUpdater updater;

    public KafkaPolicyChangeListener(ObjectMapper objectMapper, PolicyIncrementalUpdater updater) {
        this.objectMapper = objectMapper;
        this.updater = updater;
    }

    @KafkaListener(topics = "${policy.kafka.topic:policy-change}",
            groupId = "${policy.kafka.group-id:policy-search}")
    public void onMessage(String json) throws Exception {
        PolicyChangeMessage message = objectMapper.readValue(json, PolicyChangeMessage.class);
        updater.apply(toChange(message));
    }

    private PolicyChange toChange(PolicyChangeMessage message) {
        MessagePosition position = new MessagePosition(message.getPosition());
        if ("DELETE".equalsIgnoreCase(message.getOperation())) {
            return PolicyChange.delete(message.getPolicyId(), position);
        }
        if (!"UPSERT".equalsIgnoreCase(message.getOperation())) {
            throw new IllegalArgumentException("unsupported policy operation: " + message.getOperation());
        }
        if (message.getDetail() == null || message.getIndexTerms() == null) {
            throw new IllegalArgumentException("UPSERT requires detail and indexTerms");
        }
        PolicyRecord policy = new PolicyRecord(message.getPolicyId(),
                message.getDetail().getBytes(StandardCharsets.UTF_8),
                new HashSet<String>(message.getIndexTerms()));
        return PolicyChange.upsert(policy, position);
    }
}
