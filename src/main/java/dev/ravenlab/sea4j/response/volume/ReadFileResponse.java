package dev.ravenlab.sea4j.response.volume;

public class ReadFileResponse {

    private final byte[] data;

    public ReadFileResponse(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return this.data;
    }
}