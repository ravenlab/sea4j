package dev.ravenlab.sea4j;

import com.google.gson.JsonObject;
import dev.ravenlab.sea4j.response.FidResponse;
import dev.ravenlab.sea4j.response.FileWrittenResponse;
import dev.ravenlab.sea4j.response.ReadFileResponse;
import dev.ravenlab.sea4j.response.VolumeLookupResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

public class VolumeClient extends BaseUploadClient {

    protected VolumeClient(String host, int port, boolean ssl, boolean verbose, ExecutorService pool, Logger logger) {
        super(host, port, ssl, verbose, pool, logger);
    }

    public CompletableFuture<ReadFileResponse> readFile(String fid) {
        return CompletableFuture.supplyAsync(() -> {
            VolumeLookupResponse lookup = this.lookupVolume(fid);
            if(lookup == null) {
                return new ReadFileResponse();
            }
            String url = this.buildBaseString(lookup.getUrl()) + "/" + fid;
            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            try(Response response = this.getClient().newCall(request).execute()) {
                byte[] body = Objects.requireNonNull(response.body()).bytes();
                System.out.println("body: " + Arrays.toString(body));
                return new ReadFileResponse(body);
            } catch(IOException | NullPointerException e) {
                e.printStackTrace();
                return new ReadFileResponse();
            }
        }, this.getPool());
    }

    public CompletableFuture<FileWrittenResponse> writeFile(File file) {
        return CompletableFuture.supplyAsync(() -> {
            FidResponse fid = this.createFid();
            if(fid == null) {
                return null;
            }
            return this.sendFile(file, fid.getUrl(), fid.getFid());
        }, this.getPool());
    }

    public CompletableFuture<FileWrittenResponse> updateFile(File file, String fid) {
        return CompletableFuture.supplyAsync(() -> {
            VolumeLookupResponse lookup = this.lookupVolume(fid);
            if(lookup == null) {
                return null;
            }
            return this.sendFile(file, lookup.getUrl(), fid);
        });
    }

    public CompletableFuture<Boolean> deleteFile(String fid) {
        return CompletableFuture.supplyAsync(() -> {
            VolumeLookupResponse lookup = this.lookupVolume(fid);
            if(lookup == null) {
                return false;
            }
            String url = this.buildBaseString(lookup.getUrl()) + "/" + fid;
            Request request = new Request.Builder()
                    .url(url)
                    .delete()
                    .build();
            try(Response response = this.getClient().newCall(request).execute()) {
                String message = response.message();
                return message != null && message.equals("Accepted");
            } catch(IOException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    private FileWrittenResponse sendFile(File file, String hostAndPort, String fid) {
        String url = this.buildBaseString(hostAndPort) + "/" + fid;
        System.out.println(url);

        Request request = this.buildFileRequest(url, file);
        if(request == null) {
            return null;
        }
        try(Response response = this.getClient().newCall(request).execute()) {
            String json = Objects.requireNonNull(response.body()).string();
            System.out.println("response json: " + json);
            JsonObject jsonObj = this.getGson().fromJson(json, JsonObject.class);
            long fileSize = jsonObj.get("size").getAsLong();
            String eTag = jsonObj.get("eTag").getAsString();
            return new FileWrittenResponse(fid, fileSize, eTag);
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
            String reqUrl = this.buildMasterString() + "/dir/lookup?volumeId=" + volumeId;
            Request request = new Request.Builder()
                    .url(reqUrl)
                    .get()
                    .build();
            try(Response response = this.getClient().newCall(request).execute()) {
                String json = Objects.requireNonNull(response.body()).string();
                System.out.println("lookup volume: " + json);
                JsonObject obj = this.getGson().fromJson(json, JsonObject.class);
                int lookupId = obj.get("volumeId").getAsInt();
                JsonObject locations = obj.get("locations").getAsJsonArray().get(0).getAsJsonObject();
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
        String url = this.buildMasterString() + "/dir/assign";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try(Response response = this.getClient().newCall(request).execute()){
            String json = Objects.requireNonNull(response.body()).string();
            System.out.println("json: " + json);
            return this.getGson().fromJson(json, FidResponse.class);
        } catch(IOException | NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Builder extends BaseUploadClient.Builder<Builder> {

        private String masterHost = null;
        private int masterPort = -1;

        protected Builder(ExecutorService pool, Logger logger) {
            super(pool, logger);
        }

        public Builder masterHost(String host) {
            this.masterHost = host;
            return this;
        }

        public Builder masterPort(int port) {
            this.masterPort = port;
            return this;
        }

        public VolumeClient build() {
            return new VolumeClient(this.masterHost, this.masterPort,
                    this.getSsl(), this.getVerbose(),
                    this.getPool(), this.getLogger());
        }
    }
}
