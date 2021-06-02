package dev.ravenlab.sea4j;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class FilerClient extends BaseUploadClient {

    protected FilerClient(String host, int port, boolean ssl, boolean verbose, Logger logger) {
        super(host, port, ssl, verbose, logger);
    }

    public Boolean createDirectory(Path directory) {
        return null;
    }

    public Boolean uploadFile(Path directory, File file) {
        return null;
    }

    public Boolean getFile(Path directory, String fileName) {
        return null;
    }

    public Boolean deleteFile(Path directory, String fileName) {
        return null;
    }

    public Boolean listFiles(Path directory) {
        return null;
    }

    public static class Builder extends BaseUploadClient.Builder<Builder> {

        private String filerHost = null;
        private int filerPort = -1;

        protected Builder(Logger logger) {
            super(logger);
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
                    this.ssl, this.verbose, this.logger);
        }
    }
}