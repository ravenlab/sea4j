package dev.ravenlab.sea4j.test;

import dev.ravenlab.sea4j.Constant;
import dev.ravenlab.sea4j.SeaweedClient;
import dev.ravenlab.sea4j.response.FileUpdatedResponse;
import dev.ravenlab.sea4j.test.util.HashUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.OutputFrame;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SeaweedClientTest {

    private DockerComposeContainer container;
    private File testFile;
    private SeaweedClient client;

    @Before
    public void setup() {
        System.setProperty(Constant.DEBUG_KEY, "true");
        File composeFile = new File("src/test/resources/docker-compose.yml");
        this.container = new DockerComposeContainer(composeFile)
                .withExposedService("master", 9333)
                .withExposedService("master", 19333)
                .withExposedService("volume", 8080)
                .withExposedService("volume", 18080)
                .withLogConsumer("volume", (out) -> {
                    OutputFrame frame = (OutputFrame) out;
                    System.out.println("volume: " + frame.getUtf8String());
                }).withLogConsumer("master", (out) -> {
                    OutputFrame frame = (OutputFrame) out;
                    System.out.println("master: " + frame.getUtf8String());
                });
        this.container.start();
        this.testFile = new File("src/test/resources/test.txt");
        String masterHost = this.container.getServiceHost("master", 9333);
        int masterPort = this.container.getServicePort("master", 9333);
        this.client = new SeaweedClient.Builder()
                .masterHost(masterHost)
                .masterPort(masterPort)
                .verbose(true)
                .build();
    }

    @After
    public void shutdown() {
        this.container.stop();
    }

    @Test
    public void testWrite() {
        long size = this.testFile.length();
        try {
            FileUpdatedResponse response = this.client.writeFile(this.testFile).get();
            assertEquals(response.getFileSize(), size);
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRead() {
        byte[] testFileHash = HashUtil.getMD5(this.testFile);
        try {
            FileUpdatedResponse response = this.client.writeFile(this.testFile).get();
            byte[] readHash = HashUtil.getMD5(this.client.readFile(response.getFid()).get().getData());
            assertTrue(Arrays.equals(testFileHash, readHash));
        } catch(InterruptedException |ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testReadNoFile() {
        try {
            assertFalse(this.client.readFile("3,017e84ad0d").get().getExists());
        } catch(InterruptedException |ExecutionException e) {
            e.printStackTrace();
        }
    }
}