package dev.ravenlab.sea4j.response;

public class VolumeLookupResponse {

    private final int volumeId;
    private final String url;

    public VolumeLookupResponse(int volumeId, String url) {
        this.volumeId = volumeId;
        this.url = url;
    }

    public int getVolumeId() {
        return this.volumeId;
    }

    public String getUrl() {
        return this.url;
    }
}