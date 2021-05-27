package dev.ravenlab.sea4j.response;

public class FidResponse {

    private final int count;
    private final String fid;
    private final String url;

    public FidResponse(int count, String fid, String url) {
        this.count = count;
        this.fid = fid;
        this.url = url;
    }

    public int getCount() {
        return this.count;
    }

    public String getFid() {
        return this.fid;
    }

    public String getUrl() {
        return this.url;
    }
}