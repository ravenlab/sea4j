package com.github.ravenlab.sea4j;

public class SeaweedClient {

    private final String host;
    private final int port;
    private final boolean ssl;

    private SeaweedClient(String host, int port, boolean ssl) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
    }

    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }

    public boolean getSSL() {
        return this.ssl;
    }

    //Create fid
    //https://github.com/chrislusf/seaweedfs#write-file

    //Read file

    //Write file - Return fid string

    //Delete file

    public static class Builder {

        private String host = null;
        private int port = -1;
        private boolean ssl = false;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder ssl(boolean ssl) {
            this.ssl =  ssl;
            return this;
        }

        public SeaweedClient build() {
            return new SeaweedClient(this.host, this.port, this.ssl);
        }
    }
}
