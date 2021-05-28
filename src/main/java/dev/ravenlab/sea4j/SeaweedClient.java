package dev.ravenlab.sea4j;

import dev.ravenlab.sea4j.response.FidResponse;
import dev.ravenlab.sea4j.response.FileResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.ravenlab.sea4j.response.VolumeLookupResponse;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SeaweedClient {

    private final String masterHost;
    private final int masterPort;
    private final boolean ssl;
    private final ExecutorService pool;
    private final Gson gson;
    private final OkHttpClient client;

    private SeaweedClient(String masterHost, int masterPort, boolean ssl, int poolSize) {
        this.masterHost = masterHost;
        this.masterPort = masterPort;
        this.ssl = ssl;
        this.pool = new ThreadPoolExecutor(0, poolSize,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>());
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
        .addInterceptor(new Interceptor() {
            @NotNull
            @Override
            public Response intercept(@NotNull Chain chain) throws IOException {
                Request request = chain.request();
                long t1 = System.nanoTime();
                System.out.println(String.format("Sending request %s on %s%n%s",
                        request.url(), chain.connection(), request.headers()));
                Response response = chain.proceed(request);
                long t2 = System.nanoTime();
                System.out.println(String.format("Received response for %s in %.1fms%n%s",
                        response.request().url(), (t2 - t1) / 1e6d, response.headers()));
                return response;
            }
        }).build();
    }

    public CompletableFuture<byte[]> readFile(String fid) {
        return CompletableFuture.supplyAsync(() -> {
            VolumeLookupResponse lookup = this.lookupVolume(fid);
            String url = this.buildBaseString(lookup.getUrl()) + "/" + fid;
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            try(Response response = this.client.newCall(request).execute()) {
                return Objects.requireNonNull(response.body()).bytes();
            } catch(IOException | NullPointerException e) {
                e.printStackTrace();
                return new byte[0];
            }
        }, this.pool);
    }

    public CompletableFuture<FileResponse> writeFile(File file) {
        return CompletableFuture.supplyAsync(() -> {
            FidResponse fid = this.createFid();
            System.out.println(fid.getFid());
            System.out.println(fid.getUrl());
            if(fid == null) {
                return null;
            }
            return this.sendFile(file, fid.getUrl(), fid.getFid());
        }, this.pool);
    }

    public CompletableFuture<FileResponse> updateFile(File file, String fid) {
        return CompletableFuture.supplyAsync(() -> {
            VolumeLookupResponse lookup = this.lookupVolume(fid);
            return this.sendFile(file, lookup.getUrl(), fid);
        });
    }

    public CompletableFuture<String> deleteFile(String fid) {
        return CompletableFuture.supplyAsync(() -> {
            VolumeLookupResponse lookup = this.lookupVolume(fid);
            String url = this.buildBaseString(lookup.getUrl()) + "/" + fid;
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .build();
            try(Response response = this.client.newCall(request).execute()) {
                return response.message();
            } catch(IOException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private FileResponse sendFile(File file, String hostAndPort, String fid) {
        String url = this.buildBaseString("localhost:8080") + "/" + fid;
        System.out.println(url);

        Request request = this.buildFileRequest(url, file);
        if(request == null) {
            return null;
        }
        try(Response response = this.client.newCall(request).execute()) {
            String json = Objects.requireNonNull(response.body()).string();
            System.out.println("response json: " + json);
            JsonObject jsonObj = this.gson.fromJson(json, JsonObject.class);
            long fileSize = jsonObj.get("size").getAsLong();
            String eTag = jsonObj.get("eTag").getAsString();
            return new FileResponse(fid, fileSize, eTag);
        } catch(IOException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    private VolumeLookupResponse lookupVolume(String fid) {
        if(fid == null) {
            return null;
        } else if(!fid.contains(",")) {
            return null;
        }
        String[] split = fid.split(",");
        try {
            int volumeId = Integer.parseInt(split[0]);
            String reqUrl = this.buildBaseString(this.masterHost, this.masterPort) + "/dir/lookup?volumeId=" + volumeId;
            Request request = new Request.Builder()
                    .url(reqUrl)
                    .get()
                    .build();
            try(Response response = this.client.newCall(request).execute()) {
                String json = Objects.requireNonNull(response.body()).string();
                JsonObject obj = this.gson.fromJson(json, JsonObject.class);
                int lookupId = obj.get("volumeId").getAsInt();
                JsonObject locations = obj.get("locations").getAsJsonObject();
                String url = locations.get("url").getAsString();
                return new VolumeLookupResponse(lookupId, url);
            }
        } catch(NumberFormatException | IOException | NullPointerException ex) {
            return null;
        }
    }

    private Request buildFileRequest(String url, File file) {
        MediaType type = MediaType.parse("application/octet-stream");//Files.probeContentType(file.toPath()));
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(RequestBody.create(file, type))
                .build();
        return new Request.Builder()
                .url(url)
                .post(body)
                .build();
    }

    private FidResponse createFid() {
        String url = this.buildBaseString(this.masterHost, this.masterPort) + "/dir/assign";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try(Response response = this.client.newCall(request).execute()){
            String json = Objects.requireNonNull(response.body()).string();
            System.out.println("json: " + json);
            return this.gson.fromJson(json, FidResponse.class);
        } catch(IOException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String buildBaseString(String host, int port) {
        return this.buildBaseString(host + ":" + port);
    }

    private String buildBaseString(String hostAndPort) {
        StringBuilder sb = new StringBuilder();
        String protocol = "http";
        if(ssl) {
            protocol = "https";
        }
        sb.append(protocol);
        sb.append("://");
        sb.append(hostAndPort);
        return sb.toString();
    }

    public static class Builder {

        private String masterHost = null;
        private int masterPort = -1;
        private boolean ssl = false;
        private int poolSize = Integer.MAX_VALUE;

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

        public SeaweedClient build() {
            //TODO - throw exception if < 1 || host is null
            return new SeaweedClient(this.masterHost, this.masterPort, this.ssl, this.poolSize);
        }
    }
}
