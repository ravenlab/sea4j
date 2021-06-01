package dev.ravenlab.sea4j.test;

import dev.ravenlab.sea4j.Constant;
import dev.ravenlab.sea4j.SeaweedClient;
import dev.ravenlab.sea4j.VolumeClient;
import dev.ravenlab.sea4j.response.volume.FileWrittenResponse;
import dev.ravenlab.sea4j.test.util.HashUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.OutputFrame;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VolumeClientTest {

    private DockerComposeContainer container;
    private File testFile;
    private File starTestFile;
    private VolumeClient volumeClient;

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
        this.starTestFile = new File("src/test/resources/star-test.txt");
        String masterHost = this.container.getServiceHost("master", 9333);
        int masterPort = this.container.getServicePort("master", 9333);
        this.volumeClient = new SeaweedClient.Builder()
                .logger(Logger.getLogger(this.getClass().getName()))
                .build()
                .volumeBuilder()
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
            FileWrittenResponse response = this.volumeClient.writeFile(this.testFile).get();
            assertEquals(response.getFileSize(), size);
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdateFile() {
        long originalSize = this.testFile.length();
        int chars = 10;
        byte[] starFileHash = HashUtil.getMD5(this.starTestFile);
        try {
            FileWrittenResponse response = this.volumeClient.writeFile(this.testFile).get();
            String fid = response.getFid();
            assertEquals(response.getFileSize(), originalSize);
            assertEquals(chars, this.volumeClient.updateFile(this.starTestFile, fid).get().getFileSize());
            byte[] retrievedHash = HashUtil.getMD5(this.volumeClient.readFile(fid).get().getData());
            assertTrue(Arrays.equals(starFileHash, retrievedHash));
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdateFileNullVolume() {
        try {
            assertNull(this.volumeClient.updateFile(this.starTestFile, null).get());
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRead() {
        byte[] testFileHash = HashUtil.getMD5(this.testFile);
        try {
            FileWrittenResponse response = this.volumeClient.writeFile(this.testFile).get();
            byte[] readHash = HashUtil.getMD5(this.volumeClient.readFile(response.getFid()).get().getData());
            assertTrue(Arrays.equals(testFileHash, readHash));
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testReadNoFile() {
        try {
            assertFalse(this.volumeClient.readFile("3,017e84ad0d").get().getExists());
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDelete() {
        byte[] testFileHash = HashUtil.getMD5(this.testFile);
        try {
            FileWrittenResponse updatedResponse = this.volumeClient.writeFile(this.testFile).get();
            String fid = updatedResponse.getFid();
            byte[] readHash = HashUtil.getMD5(this.volumeClient.readFile(fid).get().getData());
            assertTrue(Arrays.equals(testFileHash, readHash));
            assertTrue(this.volumeClient.deleteFile(fid).get());
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDeleteNoFile() {
        try {
            assertFalse(this.volumeClient.deleteFile("3,017e84ad0d").get());
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}