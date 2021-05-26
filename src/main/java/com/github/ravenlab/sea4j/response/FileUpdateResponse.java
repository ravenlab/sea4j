package com.github.ravenlab.sea4j.response;

public class FileUpdateResponse {

    private final String fid;
    private final String filename;
    private final long fileSize;
    private final String eTag;

    public FileUpdateResponse(String fid, String fileName, long fileSize, String eTag) {
        this.fid = fid;
        this.filename = fileName;
        this.fileSize = fileSize;
        this.eTag = eTag;
    }

    public String getFid() {
        return this.fid;
    }

    public String getFileName() {
        return this.filename;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public String getEtag() {
        return this.eTag;
    }
}