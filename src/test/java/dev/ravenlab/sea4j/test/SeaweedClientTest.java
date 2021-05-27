package dev.ravenlab.sea4j.test;

import dev.ravenlab.sea4j.SeaweedClient;
import org.junit.Test;

public class SeaweedClientTest {

    @Test
    public void testConstruct() {
        SeaweedClient client = new SeaweedClient.Builder()
                .masterHost("localhost")
                .build();
    }
}