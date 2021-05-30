package dev.ravenlab.sea4j.response;

public class ReadFileResponse {

    private final byte[] data;
    private final boolean exists;

    public ReadFileResponse(byte[] data) {
        this(data, true);
    }

    public ReadFileResponse() {
        this(new byte[0], false);
    }

    public ReadFileResponse(byte[] data, boolean exists) {
        this.data = data;
        this.exists = exists;
    }

    public byte[] getData() {
        return this.data;
    }

    public boolean getExists() {
        return this.exists;
    }
}