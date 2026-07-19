package com.arch.policy.search.domain.snapshot;

import com.arch.policy.common.model.MessagePosition;
import com.arch.policy.common.model.PolicyRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.arch.policy.search.domain.snapshot.SnapshotPorts.*;
import static org.junit.jupiter.api.Assertions.*;

class SnapshotLifecycleTest {
    @TempDir Path root;

    @Test
    void buildsFullDataReplaysChangesAndValidatesPosition() throws Exception {
        SnapshotBuilder builder = builder(false);

        PolicySnapshot snapshot = builder.build("V101");

        assertEquals(new MessagePosition(12), snapshot.getPosition());
        assertArrayEquals(bytes("new-detail"), snapshot.findDetail(1));
        assertEquals(1, snapshot.findPolicyIds("active").getCardinality());
        assertEquals(0, snapshot.findPolicyIds("draft").getCardinality());
        snapshot.closeAndDelete();
    }

    @Test
    void switchWaitsForOldQueriesBeforeClosingAndDeletingOldVersion() throws Exception {
        ActiveSnapshotRegistry registry = new ActiveSnapshotRegistry();
        PolicySnapshot old = builder(false).build("V100");
        PolicySnapshot replacement = builder(false).build("V101");
        registry.activate(old);
        ActiveSnapshotRegistry.SnapshotLease oldQuery = registry.acquire();

        registry.activate(replacement);

        assertTrue(Files.exists(old.getDirectory()));
        assertArrayEquals(bytes("new-detail"), oldQuery.snapshot().findDetail(1));
        try (ActiveSnapshotRegistry.SnapshotLease newQuery = registry.acquire()) {
            assertEquals("V101", newQuery.snapshot().getVersion());
        }
        oldQuery.close();
        assertFalse(Files.exists(old.getDirectory()));
        assertThrows(IllegalStateException.class, () -> old.findPolicyIds("active"));
        registry.close();
    }

    @Test
    void failedRuntimeCandidateDoesNotReplaceServingSnapshot() throws Exception {
        ActiveSnapshotRegistry registry = new ActiveSnapshotRegistry();
        registry.activate(builder(false).build("V100"));
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        PolicySnapshotService service = new PolicySnapshotService(builder(true), registry, executor, 1);

        assertTrue(service.refresh("V101"));
        waitUntilBuildFinishes(service);

        assertTrue(service.isReady());
        try (ActiveSnapshotRegistry.SnapshotLease query = registry.acquire()) {
            assertEquals("V100", query.snapshot().getVersion());
        }
        assertFalse(Files.exists(root.resolve("V101")));
        service.close();
    }

    private SnapshotBuilder builder(final boolean failValidation) {
        FullPolicyLoader full = target -> {
            target.upsert(new PolicyRecord(1, bytes("old-detail"), Collections.singleton("draft")));
            return new MessagePosition(10);
        };
        IncrementalReplayer replay = new IncrementalReplayer() {
            @Override public MessagePosition latestPosition() { return new MessagePosition(12); }
            @Override public MessagePosition replayInto(PolicySnapshot target, MessagePosition from,
                                                         MessagePosition latest) throws Exception {
                target.upsert(new PolicyRecord(1, bytes("new-detail"),
                        Collections.singleton("active")));
                return latest;
            }
        };
        SnapshotValidator validator = (candidate, position) -> {
            new DefaultSnapshotValidator().validate(candidate, position);
            if (failValidation) throw new IllegalStateException("invalid candidate");
        };
        SnapshotDirectory directories = new SnapshotDirectory() {
            @Override public Path create(String version) throws Exception {
                Path path = root.resolve(version);
                Files.createDirectories(path);
                return path;
            }
            @Override public void delete(Path directory) throws Exception {
                if (!Files.exists(directory)) return;
                Files.walk(directory).sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (Exception failure) { throw new RuntimeException(failure); }
                });
            }
        };
        return new SnapshotBuilder(full, replay, validator, directories);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void waitUntilBuildFinishes(PolicySnapshotService service) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (!service.isBuilding()) return;
            Thread.sleep(10);
        }
        fail("candidate build did not finish");
    }
}
