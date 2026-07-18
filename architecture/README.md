# Policy snapshot lifecycle

This module implements version-isolated policy snapshots. Each snapshot owns a RocksDB detail
store, a bitmap index, and the incremental-message position at which both were built.

## Integration flow

1. Implement `FullPolicyLoader` to stream the full-data cut into `PolicySnapshot.upsert` and
   return the cut's message position.
2. Implement `IncrementalReplayer` to apply upserts/deletes after that position. The builder
   repeats replay until it reaches a stable latest position.
3. Supply `DefaultSnapshotValidator` (or a stricter domain validator), a
   `FileSystemSnapshotDirectory`, and construct `SnapshotBuilder`.
4. Call `PolicySnapshotService.start(version)` during startup. Keep the application's readiness
   probe bound to `service.isReady()`; failures remain unready and retry.
5. Call `refresh(newVersion)` at runtime. A failed candidate is discarded. A valid candidate is
   atomically activated while the old snapshot continues serving existing leases.
6. Every search must use `try (SnapshotLease lease = registry.acquire())`. Releasing the last old
   lease closes RocksDB and deletes that retired version's directory.

The module intentionally leaves message-broker and full-data-source clients behind interfaces so
the snapshot consistency rules are independent of Kafka, HTTP, database, or framework choices.

## Package layout

- `api`: serializable Dubbo contract and request/response DTOs.
- `rpc`: Dubbo search provider and supplier callback provider.
- `aggregation`: Redis-backed fan-out/fan-in search coordination and local waiters.
- `redis`: atomic Lua state transitions and Pub/Sub early wake-up.
- `demo`: asynchronous downstream supplier simulation.
- `kafka`: JSON policy-change consumer and message DTO.
- `application`: startup and incremental-update use cases.
- root `policy` package: versioned RocksDB/Bitmap snapshot domain and lifecycle.

Kafka messages use a globally monotonic `position` so duplicate/out-of-order delivery is ignored.
If the topic has multiple partitions, the producer must supply this global sequence; otherwise the
position model should be replaced with a per-partition offset map.

## Async supplier search

`asyncSearch` initializes pending suppliers, state and result TTL atomically in Redis, registers a
local waiter, double-checks Redis, then dispatches all supplier tasks. Supplier callbacks append
result chunks and remove a supplier from the pending set only on its final callback. The Lua script
sets `COMPLETED` and publishes `search-finished` when the last supplier finishes. Pub/Sub only wakes
the local waiter early; Redis remains the source of truth and is checked every 200 ms. Timeout is
also a Lua state transition and returns all partial results already recorded.
