package dev.ravenlab.sea4j;

import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class FilerClient extends BaseUploadClient {

    protected FilerClient(String host, int port, boolean ssl, boolean verbose, ExecutorService pool, Logger logger) {
        super(host, port, ssl, verbose, pool, logger);
    }

    public static class Builder extends BaseUploadClient.Builder<Builder> {

        private String filerHost = null;
        private int filerPort = -1;

        protected Builder(ExecutorService pool, Logger logger) {
            super(pool, logger);
        }

        public Builder filerHost(String host) {
            this.filerHost = host;
            return this;
        }

        public Builder filerPort(int port) {
            this.filerPort = port;
            return this;
        }

        public FilerClient build() {
            return new FilerClient(this.filerHost, this.filerPort,
                    this.ssl, this.verbose,
                    this.pool, this.logger);
        }
    }
}