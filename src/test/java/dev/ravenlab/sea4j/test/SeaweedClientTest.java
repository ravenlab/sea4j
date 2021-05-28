package dev.ravenlab.sea4j.test;

import dev.ravenlab.sea4j.SeaweedClient;
import dev.ravenlab.sea4j.response.FileResponse;
import dev.ravenlab.sea4j.test.util.HashUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.OutputFrame;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class SeaweedClientTest {

    private DockerComposeContainer container;
    private File testFile;

    @Before
    public void setup() {
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
                });;
        this.container.start();
        this.testFile = new File("src/test/resources/test.txt");
    }

    @After
    public void shutdown() {
        this.container.stop();
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
        String masterHost = this.container.getServiceHost("master", 9333);
        int masterPort = this.container.getServicePort("master", 9333);
        System.out.println(masterHost + " " + masterPort);
        SeaweedClient client = new SeaweedClient.Builder()
                .masterHost(masterHost)
                .masterPort(masterPort)
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