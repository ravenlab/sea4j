package dev.ravenlab.sea4j.test;

import dev.ravenlab.sea4j.SeaweedClient;
import dev.ravenlab.sea4j.response.FileResponse;
import dev.ravenlab.sea4j.test.util.HashUtil;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class SeaweedClientTest {

    @Test
    public void testConstruct() {
        new SeaweedClient.Builder()
                .masterHost("localhost")
                .masterPort(9333)
                .build();
    }

    @Test
    public void testWrite() {
        SeaweedClient client = new SeaweedClient.Builder()
                .masterHost("localhost")
                .masterPort(9333)
                .build();
        File testFile = new File("test.txt");
        long size = testFile.length();
        try {
            FileResponse response = client.writeFile(testFile).get();
            assertEquals(response.getFileSize(), size);
        } catch(InterruptedException |ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRead() {
        SeaweedClient client = new SeaweedClient.Builder()
                .masterHost("localhost")
                .masterPort(9333)
                .build();
        File testFile = new File("test.txt");
        byte[] testFileHash = HashUtil.getMD5(testFile);
        try {
            FileResponse response = client.writeFile(testFile).get();

        } catch(InterruptedException |ExecutionException e) {
            e.printStackTrace();
        }
    }
}