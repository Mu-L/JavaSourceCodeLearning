package com.arch.policy.search.application;

import com.arch.policy.search.domain.snapshot.PolicySnapshotService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public final class PolicyStartupRunner implements ApplicationRunner {
    private final PolicySnapshotService snapshotService;
    private final String initialVersion;

    public PolicyStartupRunner(PolicySnapshotService snapshotService, String initialVersion) {
        this.snapshotService = snapshotService;
        this.initialVersion = initialVersion;
    }

    @Override public void run(ApplicationArguments args) throws Exception {
        snapshotService.initializeBlocking(initialVersion);
    }
}
