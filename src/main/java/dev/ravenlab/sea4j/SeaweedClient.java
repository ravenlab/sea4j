package dev.ravenlab.sea4j;

import com.google.gson.Gson;
import dev.ravenlab.sea4j.util.DebugUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SeaweedClient {

    private final String masterHost;
    private final int masterPort;
    private final boolean ssl;
    private final ExecutorService pool;
    private final Gson gson;
    private final OkHttpClient client;
    private final Logger logger;
    private final VolumeClient volume;

    private SeaweedClient(String masterHost, int masterPort,
                          boolean ssl, int maxPoolSize,
                          boolean verbose, Logger logger) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.ssl = ssl;
        this.pool = new ThreadPoolExecutor(0, maxPoolSize,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        this.gson = new Gson();
        this.client = this.buildClient(verbose);
        this.logger = logger;
        this.volume = new VolumeClient(this);
    }

    public VolumeClient volume() {
        return this.volume;
    }

    protected OkHttpClient getClient() {
        return this.client;
    }

    protected ExecutorService getPool() {
        return this.pool;
    }

    protected Gson getGson() {
        return this.gson;
    }

    protected String buildMasterString() {
        return this.buildBaseString(this.masterHost, this.masterPort);
    }

    protected String buildBaseString(String host, int port) {
        return this.buildBaseString(host + ":" + port);
    }

    protected String buildBaseString(String hostAndPort) {
        StringBuilder sb = new StringBuilder();
        String protocol = "http";
        if(this.ssl) {
            protocol = "https";
        }
        sb.append(protocol);
        sb.append("://");
        sb.append(DebugUtil.checkForDebugUrl(hostAndPort));
        return sb.toString();
    }

    private OkHttpClient buildClient(boolean verbose) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if(verbose) {
            builder.addInterceptor(chain -> {
                Request request = chain.request();
                long t1 = System.nanoTime();
                this.logger.log(Level.INFO, String.format("Sending request %s on %s%n%s",
                        request.url(), chain.connection(), request.headers()));
                Response response = chain.proceed(request);
                long t2 = System.nanoTime();
                this.logger.log(Level.INFO, String.format("Received response for %s in %.1fms%n%s",
                        response.request().url(), (t2 - t1) / 1e6d, response.headers()));
                return response;
            });
        }
        return builder.build();
    }


    public static class Builder {

        private String masterHost = null;
        private int masterPort = -1;
        private boolean ssl = false;
        private int poolSize = Integer.MAX_VALUE;
        private boolean verbose = false;
        private Logger logger = Logger.getLogger(SeaweedClient.class.getName());

        public Builder masterHost(String host) {
            this.masterHost = host;
            return this;
        }

        public Builder masterPort(int port) {
            this.masterPort = port;
            return this;
        }

        public Builder ssl(boolean ssl) {
            this.ssl =  ssl;
            return this;
        }

        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public SeaweedClient build() {
            //TODO - throw exception if < 1 || host is null
            return new SeaweedClient(this.masterHost, this.masterPort, this.ssl, this.poolSize, this.verbose, logger);
        }
    }
}
