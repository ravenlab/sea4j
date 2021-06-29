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
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VolumeClientTest {

    private File composeFile;
    private DockerComposeContainer container;
    private File testFile;
    private File starTestFile;
    private VolumeClient volumeClient;

    @Before
    public void setup() {
        System.setProperty(Constant.DEBUG_KEY, "true");
        File srcFolder = new File("src");
        File testFolder = new File(srcFolder, "test");
        File resourceFolder = new File(testFolder, "resources");
        this.composeFile = new File(resourceFolder, "docker-compose.yml");
        this.testFile = new File(resourceFolder, "test.txt");
        this.starTestFile = new File(resourceFolder, "star-test.txt");
        if(this.composeFile.exists()) {
            this.container = new DockerComposeContainer(this.composeFile)
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
    }

    @After
    public void shutdown() {
        if(this.container != null) {
            this.container.stop();
        }
    }

    @Test
    public void testWrite() {
        long size = this.testFile.length();
        try {
            FileWrittenResponse response = this.volumeClient.writeFile(this.testFile);
            assertEquals(response.getFileSize(), size);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdateFile() {
        long originalSize = this.testFile.length();
        int chars = 10;
        byte[] starFileHash = HashUtil.getMD5(this.starTestFile);
        try {
            FileWrittenResponse response = this.volumeClient.writeFile(this.testFile);
            String fid = response.getFid();
            assertEquals(response.getFileSize(), originalSize);
            assertEquals(chars, this.volumeClient.updateFile(this.starTestFile, fid).getFileSize());
            byte[] retrievedHash = HashUtil.getMD5(this.volumeClient.readFile(fid).getData());
            assertTrue(Arrays.equals(starFileHash, retrievedHash));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdateFileNullVolume() {
        try {
            assertNull(this.volumeClient.updateFile(this.starTestFile, null));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRead() {
        byte[] testFileHash = HashUtil.getMD5(this.testFile);
        try {
            FileWrittenResponse response = this.volumeClient.writeFile(this.testFile);
            byte[] readHash = HashUtil.getMD5(this.volumeClient.readFile(response.getFid()).getData());
            assertTrue(Arrays.equals(testFileHash, readHash));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testReadNoFile() {
        try {
            assertNull(this.volumeClient.readFile("3,017e84ad0d"));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDelete() {
        byte[] testFileHash = HashUtil.getMD5(this.testFile);
        try {
            FileWrittenResponse updatedResponse = this.volumeClient.writeFile(this.testFile);
            String fid = updatedResponse.getFid();
            byte[] readHash = HashUtil.getMD5(this.volumeClient.readFile(fid).getData());
            assertTrue(Arrays.equals(testFileHash, readHash));
            assertTrue(this.volumeClient.deleteFile(fid));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDeleteNoFile() {
        try {
            assertFalse(this.volumeClient.deleteFile("3,017e84ad0d"));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}