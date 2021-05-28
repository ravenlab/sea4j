package dev.ravenlab.sea4j.test;

import dev.ravenlab.sea4j.SeaweedClient;
import dev.ravenlab.sea4j.response.FileResponse;
import dev.ravenlab.sea4j.test.util.HashUtil;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class SeaweedClientTest {

    private DockerComposeContainer container;
    private File testFile;

    @Before
    public void setup() {
        File composeFile = new File("src/test/resources/compose.yml");
        this.container = new DockerComposeContainer(composeFile)
        .withExposedService("master", 9333)
        .withExposedService("volume", 8080);
        this.container.start();
        this.testFile = new File("src/test/resources/test.txt");
    }

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
                .masterHost(this.container.getServiceHost("master", 9333))
                .masterPort(this.container.getServicePort("master", 9333))
                .build();
        long size = this.testFile.length();
        try {
            FileResponse response = client.writeFile(this.testFile).get();
            assertEquals(response.getFileSize(), size);
        } catch(InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRead() {
        SeaweedClient client = new SeaweedClient.Builder()
                .masterHost("localhost")
                .masterPort(9333)
                .build();
        byte[] testFileHash = HashUtil.getMD5(this.testFile);
        try {
            FileResponse response = client.writeFile(this.testFile).get();

        } catch(InterruptedException |ExecutionException e) {
            e.printStackTrace();
        }
    }
}