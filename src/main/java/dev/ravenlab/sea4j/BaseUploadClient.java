package dev.ravenlab.sea4j;

import com.google.gson.Gson;
import dev.ravenlab.sea4j.util.DebugUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseUploadClient {

    private final String masterHost;
    private final int masterPort;
    private final boolean ssl;
    private final Gson gson;
    private final OkHttpClient client;
    private final Logger logger;

    public BaseUploadClient(String host, int port, boolean ssl, boolean verbose, Logger logger) {
        this.masterHost = host;
        this.masterPort = port;
        this.ssl = ssl;
        this.gson = new Gson();
        this.client = this.buildClient(verbose);
        this.logger = logger;
    }

    protected OkHttpClient getClient() {
        return this.client;
    }

    protected Gson getGson() {
        return this.gson;
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

    public static class Builder<T extends Builder> {

        protected final Logger logger;
        protected boolean ssl;
        protected boolean verbose;

        protected Builder(Logger logger) {
            this.logger = logger;
            this.ssl = false;
            this.verbose = false;
        }

        public T ssl(boolean ssl) {
            this.ssl =  ssl;
            return (T) this;
        }

        public T verbose(boolean verbose) {
            this.verbose = verbose;
            return (T) this;
        }
    }
}
