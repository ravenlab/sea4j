package dev.ravenlab.sea4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SeaweedClient {

    private final ExecutorService pool;
    private final Logger logger;

    private SeaweedClient(int maxPoolSize, Logger logger) {
        this.pool = new ThreadPoolExecutor(0, maxPoolSize,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        this.logger = logger;
    }

    public VolumeClient.Builder volumeBuilder() {
        return new VolumeClient.Builder(this.pool, this.logger);
    }

    public static class Builder {

        private int poolSize = Integer.MAX_VALUE;
        private Logger logger = Logger.getLogger(SeaweedClient.class.getName());

        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public SeaweedClient build() {
            return new SeaweedClient(this.poolSize, this.logger);
        }
    }
}
