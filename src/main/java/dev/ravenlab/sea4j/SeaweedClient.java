package dev.ravenlab.sea4j;

import java.util.logging.Logger;

public class SeaweedClient {

    private final Logger logger;

    private SeaweedClient(Logger logger) {
        this.logger = logger;
    }

    public VolumeClient.Builder volumeBuilder() {
        return new VolumeClient.Builder(this.logger);
    }

    public FilerClient.Builder filerBuilder() {
        return new FilerClient.Builder(this.logger);
    }

    public static class Builder {

        private Logger logger = Logger.getLogger(SeaweedClient.class.getName());

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public SeaweedClient build() {
            return new SeaweedClient(this.logger);
        }
    }
}