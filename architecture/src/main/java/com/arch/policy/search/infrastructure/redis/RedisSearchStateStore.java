package com.arch.policy.search.infrastructure.redis;

import com.arch.policy.search.application.SearchState;
import com.arch.policy.search.application.SearchStateStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class RedisSearchStateStore implements SearchStateStore {
    public static final String FINISHED_CHANNEL = "search-finished";
    private static final String EMPTY_RESULT_MARKER = "__SEARCH_INITIALIZED__";
    private static final DefaultRedisScript<Long> INIT = script(
            "redis.call('DEL', KEYS[1], KEYS[2], KEYS[3]); redis.call('RPUSH', KEYS[1], ARGV[2]); " +
            "redis.call('EXPIRE', KEYS[1], ARGV[1]); " +
            "for i=3,#ARGV do redis.call('SADD', KEYS[2], ARGV[i]); end; " +
            "if redis.call('SCARD', KEYS[2]) == 0 then " +
            " redis.call('SET', KEYS[3], 'COMPLETED', 'EX', ARGV[1]); return 1; " +
            "end; redis.call('SET', KEYS[3], 'WAITING', 'EX', ARGV[1]); " +
            "redis.call('EXPIRE', KEYS[1], ARGV[1]); redis.call('EXPIRE', KEYS[2], ARGV[1]); return 0;");
    private static final DefaultRedisScript<Long> CALLBACK = script(
            "if redis.call('GET', KEYS[3]) ~= 'WAITING' then return 0; end; " +
            "if redis.call('SISMEMBER', KEYS[2], ARGV[2]) == 0 then return 0; end; " +
            "if ARGV[1] ~= '' then redis.call('RPUSH', KEYS[1], ARGV[1]); end; " +
            "local removed=0; if ARGV[3] == 'true' then removed=redis.call('SREM', KEYS[2], ARGV[2]); end; " +
            "redis.call('EXPIRE', KEYS[1], ARGV[5]); redis.call('EXPIRE', KEYS[2], ARGV[5]); " +
            "redis.call('EXPIRE', KEYS[3], ARGV[5]); " +
            "if removed > 0 and redis.call('SCARD', KEYS[2]) == 0 then " +
            " redis.call('SET', KEYS[3], 'COMPLETED', 'EX', ARGV[5]); " +
            " redis.call('PUBLISH', ARGV[6], ARGV[4]); return 2; end; return 1;");
    private static final DefaultRedisScript<Long> TIMEOUT = script(
            "if redis.call('GET', KEYS[3]) == 'WAITING' then " +
            " redis.call('SET', KEYS[3], 'TIMED_OUT', 'EX', ARGV[1]); " +
            " redis.call('EXPIRE', KEYS[1], ARGV[1]); redis.call('EXPIRE', KEYS[2], ARGV[1]); return 1; end; return 0;");

    private final StringRedisTemplate redis;

    public RedisSearchStateStore(StringRedisTemplate redis) { this.redis = redis; }

    @Override public void initialize(String searchKey, Set<String> supplierIds, long ttlSeconds) {
        List<String> args = new ArrayList<String>();
        args.add(Long.toString(ttlSeconds));
        args.add(EMPTY_RESULT_MARKER);
        args.addAll(supplierIds);
        redis.execute(INIT, keys(searchKey), args.toArray());
    }

    @Override public SearchState getState(String searchKey) {
        String state = redis.opsForValue().get(stateKey(searchKey));
        return state == null ? SearchState.TIMED_OUT : SearchState.valueOf(state);
    }

    @Override public List<String> getResultPayloads(String searchKey) {
        List<String> values = redis.opsForList().range(resultKey(searchKey), 0, -1);
        if (values == null) return Collections.emptyList();
        List<String> results = new ArrayList<String>(values);
        results.remove(EMPTY_RESULT_MARKER);
        return results;
    }

    @Override public void recordCallback(String searchKey, String supplierId, String payload,
                                         boolean finished, long ttlSeconds) {
        redis.execute(CALLBACK, keys(searchKey), payload, supplierId, Boolean.toString(finished),
                searchKey, Long.toString(ttlSeconds), FINISHED_CHANNEL);
    }

    @Override public void markTimedOut(String searchKey, long ttlSeconds) {
        redis.execute(TIMEOUT, keys(searchKey), Long.toString(ttlSeconds));
    }

    private static List<String> keys(String searchKey) {
        return Arrays.asList(resultKey(searchKey), pendingKey(searchKey), stateKey(searchKey));
    }
    private static String resultKey(String key) { return "search:" + key + ":results"; }
    private static String pendingKey(String key) { return "search:" + key + ":pending"; }
    private static String stateKey(String key) { return "search:" + key + ":state"; }
    private static DefaultRedisScript<Long> script(String source) {
        return new DefaultRedisScript<Long>(source, Long.class);
    }
}
