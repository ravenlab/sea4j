package dev.ravenlab.sea4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SeaweedClient {

    private final ExecutorService pool;
    private final Logger logger;

    private SeaweedClient(int idlePoolSize, int maxPoolSize, Logger logger) {
        this.pool = new ThreadPoolExecutor(idlePoolSize, maxPoolSize,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        this.logger = logger;
    }

    public VolumeClient.Builder volumeBuilder() {
        return new VolumeClient.Builder(this.pool, this.logger);
    }

    public static class Builder {

        private int idlePoolSize = 0;
        private int maxPoolSize = Integer.MAX_VALUE;
        private Logger logger = Logger.getLogger(SeaweedClient.class.getName());

        public Builder idlePoolSize(int poolSize) {
            this.idlePoolSize = poolSize;
            return this;
        }

        public Builder maxPoolSize(int poolSize) {
            this.maxPoolSize = poolSize;
            return this;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public SeaweedClient build() {
            return new SeaweedClient(this.idlePoolSize, this.maxPoolSize, this.logger);
        }
    }
}