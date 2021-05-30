package dev.ravenlab.sea4j.response;

public class FileUpdatedResponse {

    private final String fid;
    private final long fileSize;
    private final String eTag;

    public FileUpdatedResponse(String fid, long fileSize, String eTag) {
        this.fid = fid;
        this.fileSize = fileSize;
        this.eTag = eTag;
    }

    public String getFid() {
        return this.fid;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public String getEtag() {
        return this.eTag;
    }
}