package com.github.ravenlab.sea4j;

import com.github.ravenlab.sea4j.response.FileUpdateResponse;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
    private final OkHttpClient client;

    private SeaweedClient(String host, int port, boolean ssl, int poolSize) {
        this.host = host;
        this.port = port;
        this.ssl = ssl;
        this.pool = new ThreadPoolExecutor(0, poolSize,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        this.gson = new Gson();
        this.client = new OkHttpClient();
    }

    public CompletableFuture<FileUpdateResponse> writeFile(File file) {
        return CompletableFuture.supplyAsync(() -> {
            String fid = this.createFid();
            return this.sendFile(file, fid);
        }, this.pool);
    }

    public CompletableFuture<FileUpdateResponse> updateFile(File file, String fid) {
        return CompletableFuture.supplyAsync(() -> this.sendFile(file, fid));
    }

    private FileUpdateResponse sendFile(File file, String fid) {
        try {
            StringBuilder sb = new StringBuilder(this.buildBaseString());
            sb.append("/");
            sb.append(fid);
            Request request = this.buildFileRequest(sb.toString(), file);
            try(Response response = this.client.newCall(request).execute()) {
                String json = response.body().string();
                JsonObject jsonObj = this.gson.fromJson(json, JsonObject.class);
                String fileName = jsonObj.get("name").getAsString();
                long fileSize = jsonObj.get("size").getAsLong();
                String eTag = jsonObj.get("etag").getAsString();
                return new FileUpdateResponse(fid, fileName, fileSize, eTag);
            }
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Request buildFileRequest(String url, File file) {
        try {
            MediaType type = MediaType.parse(Files.probeContentType(file.toPath()));
            RequestBody body = new MultipartBody.Builder()
                    .addFormDataPart("file", file.getName(), RequestBody.create(file, type))
                    .setType(MultipartBody.FORM)
                    .build();
            return new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
        } catch(IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String createFid() {
        StringBuilder sb = new StringBuilder(this.buildBaseString());
        sb.append("/dir/assign");
        Request request = new Request.Builder()
                .url(sb.toString())
                .get()
                .build();
        try(Response response = this.client.newCall(request).execute()){
            JsonObject jsonObj = this.gson.fromJson(response.body().string(), JsonObject.class);
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

    //Update file

    //Read file

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
