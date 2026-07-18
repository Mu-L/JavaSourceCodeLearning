package com.arch.policy;

public final class PolicyChange {
    public enum Type { UPSERT, DELETE }

    private final Type type;
    private final int policyId;
    private final PolicyRecord policy;
    private final MessagePosition position;

    private PolicyChange(Type type, int policyId, PolicyRecord policy, MessagePosition position) {
        this.type = type;
        this.policyId = policyId;
        this.policy = policy;
        this.position = position;
    }

    public static PolicyChange upsert(PolicyRecord policy, MessagePosition position) {
        return new PolicyChange(Type.UPSERT, policy.getId(), policy, position);
    }

    public static PolicyChange delete(int policyId, MessagePosition position) {
        return new PolicyChange(Type.DELETE, policyId, null, position);
    }

    public Type getType() { return type; }
    public int getPolicyId() { return policyId; }
    public PolicyRecord getPolicy() { return policy; }
    public MessagePosition getPosition() { return position; }
}
