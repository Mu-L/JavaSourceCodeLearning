package com.arch.policy.common.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class PolicyRecord {
    private final int id;
    private final byte[] detail;
    private final Set<String> indexTerms;

    public PolicyRecord(int id, byte[] detail, Set<String> indexTerms) {
        if (id < 0) {
            throw new IllegalArgumentException("policy id must be non-negative");
        }
        this.id = id;
        this.detail = Arrays.copyOf(detail, detail.length);
        this.indexTerms = Collections.unmodifiableSet(new HashSet<String>(indexTerms));
    }

    public int getId() { return id; }

    public byte[] getDetail() { return Arrays.copyOf(detail, detail.length); }

    public Set<String> getIndexTerms() { return indexTerms; }
}
