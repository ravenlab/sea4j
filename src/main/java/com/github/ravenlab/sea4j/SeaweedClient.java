package com.github.ravenlab.sea4j;

import com.github.ravenlab.sea4j.response.WriteFileResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jdk.internal.util.xml.impl.Input;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SeaweedClient {

    private final String host;
    private final int port;
    private final boolean ssl;
    private final ExecutorService pool;
    private final Gson gson;

    private SeaweedClient(String host, int port, boolean ssl, int poolSize) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.pool = new ThreadPoolExecutor(0, poolSize,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        this.gson = new Gson();
    }

    public CompletableFuture<WriteFileResponse> writeFile(Path path) {
        return this.writeFile(path.toFile());
    }

    public CompletableFuture<WriteFileResponse> writeFile(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String fid = this.createFid();
                StringBuilder sb = new StringBuilder(this.buildBaseString());
                sb.append("/");
                sb.append(fid);
                URL url = new URL(sb.toString());
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Connection", "Keep-Alive");
                String boundary = UUID.randomUUID().toString() + "*";
                con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                con.setConnectTimeout(5000);
                con.setReadTimeout(5000);

                //Write output here


                InputStream inputStream = new BufferedInputStream(con.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String json = reader.readLine();
                JsonObject jsonObj = this.gson.fromJson(json, JsonObject.class);
                String fileName = jsonObj.get("name").getAsString();
                long fileSize = jsonObj.get("size").getAsLong();
                String eTag = jsonObj.get("etag").getAsString();
                return new WriteFileResponse(fid, fileName, fileSize, eTag);
            } catch(IOException e) {
                e.printStackTrace();
                return null;
            }
        }, this.pool);
    }

    private String createFid() throws MalformedURLException {
        try {
            StringBuilder sb = new StringBuilder(this.buildBaseString());
            sb.append("/dir/assign");
            URL url = new URL(sb.toString());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            InputStream inputStream = new BufferedInputStream(con.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String json = reader.readLine();
            JsonObject jsonObj = this.gson.fromJson(json, JsonObject.class);
            reader.close();
            inputStream.close();
            JsonElement fid = jsonObj.get("fid");
            if(fid == null) {
                return null;
            }
            return fid.getAsString();
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String buildBaseString() {
        StringBuilder sb = new StringBuilder();
        String protocol = "http";
        if(ssl) {
            protocol = "https";
        }
        sb.append(protocol);
        sb.append("://");
        sb.append(this.host);
        sb.append(":");
        sb.append(this.port);
        return sb.toString();
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
        private int poolSize = Integer.MAX_VALUE;

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

        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public SeaweedClient build() {
            //TODO - throw exception if < 1 || host is null
            return new SeaweedClient(this.host, this.port, this.ssl, this.poolSize);
        }
    }
}
