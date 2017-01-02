package org.votingsystem.serviceprovider.util;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CacheStats {

    private final long hitCount;
    private final long missCount;
    private final long loadSuccessCount;
    private final long loadExceptionCount;
    private final long totalLoadTime;
    private final long evictionCount;

    public CacheStats(com.google.common.cache.CacheStats cacheStats) {
        this.hitCount = cacheStats.hitCount();
        this.missCount = cacheStats.missCount();
        this.loadSuccessCount = cacheStats.loadSuccessCount();
        this.loadExceptionCount = cacheStats.loadExceptionCount();
        this.totalLoadTime = cacheStats.totalLoadTime();
        this.evictionCount = cacheStats.evictionCount();
    }

    public long getHitCount() {
        return hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public long getLoadExceptionCount() {
        return loadExceptionCount;
    }

    public long getEvictionCount() {
        return evictionCount;
    }

    public long getLoadSuccessCount() {
        return loadSuccessCount;
    }

    public long getTotalLoadTime() {
        return totalLoadTime;
    }
}
