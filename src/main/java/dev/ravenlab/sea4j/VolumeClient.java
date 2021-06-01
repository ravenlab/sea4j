package dev.ravenlab.sea4j;

import com.google.gson.JsonObject;
import dev.ravenlab.sea4j.response.volume.FidResponse;
import dev.ravenlab.sea4j.response.volume.FileWrittenResponse;
import dev.ravenlab.sea4j.response.volume.ReadFileResponse;
import dev.ravenlab.sea4j.response.volume.VolumeLookupResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

public class VolumeClient extends BaseUploadClient {

    protected VolumeClient(String host, int port, boolean ssl, boolean verbose, Logger logger) {
        super(host, port, ssl, verbose, logger);
    }

    public ReadFileResponse readFile(String fid) throws IOException {
        VolumeLookupResponse lookup = this.lookupVolume(fid);
        if(lookup == null) {
            return null;
        }
        String url = this.buildBaseString(lookup.getUrl()) + "/" + fid;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try(Response response = this.getClient().newCall(request).execute()) {
            ResponseBody body = response.body();
            if(body == null) {
                return null;
            }


            byte[] bodyBytes = body.bytes();
            System.out.println("body: " + Arrays.toString(bodyBytes));
            return new ReadFileResponse(bodyBytes);
        }
    }

    public FileWrittenResponse writeFile(File file) throws IOException {
        FidResponse fid = this.createFid();
        if(fid == null) {
            return null;
        }
        return this.sendFile(file, fid.getUrl(), fid.getFid());
    }

    public FileWrittenResponse updateFile(File file, String fid) throws IOException {
        VolumeLookupResponse lookup = this.lookupVolume(fid);
        if(lookup == null) {
            return null;
        }
        return this.sendFile(file, lookup.getUrl(), fid);
    }

    public Boolean deleteFile(String fid) throws IOException {
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
        }
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

    private VolumeLookupResponse lookupVolume(String fid) throws IOException {
        if(fid == null) {
            return null;
        } else if(!fid.contains(",")) {
            return null;
        }
        String[] split = fid.split(",");
        int volumeId = -1;
        try {
            volumeId = Integer.parseInt(split[0]);
        } catch(NumberFormatException ex) {
            ex.printStackTrace();
            return null;
        }
        String reqUrl = this.buildMasterString() + "/dir/lookup?volumeId=" + volumeId;
        Request request = new Request.Builder()
                .url(reqUrl)
                .get()
                .build();
        try(Response response = this.getClient().newCall(request).execute()) {
            ResponseBody body = response.body();
            if(body == null) {
                return null;
            }
            String json = body.string();
            System.out.println("lookup volume: " + json);
            JsonObject obj = this.getGson().fromJson(json, JsonObject.class);
            if(obj.has("error")) {
                return null;
            }
            int lookupId = obj.get("volumeId").getAsInt();
            JsonObject locations = obj.get("locations").getAsJsonArray().get(0).getAsJsonObject();
            String url = locations.get("url").getAsString();
            return new VolumeLookupResponse(lookupId, url);
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

    private FidResponse createFid() throws IOException {
        String url = this.buildMasterString() + "/dir/assign";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try(Response response = this.getClient().newCall(request).execute()) {
            ResponseBody body = response.body();
            String json = body.string();
            System.out.println("json: " + json);
            return this.getGson().fromJson(json, FidResponse.class);
        }
    }

    public static class Builder extends BaseUploadClient.Builder<Builder> {

        private String masterHost = null;
        private int masterPort = -1;

        protected Builder(Logger logger) {
            super(logger);
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
                    this.ssl, this.verbose, this.logger);
        }
    }
}
