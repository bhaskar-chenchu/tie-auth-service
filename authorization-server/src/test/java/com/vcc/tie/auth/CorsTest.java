package com.vcc.tie.auth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertTrue;

@ActiveProfiles("unittest")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CorsTest {

    @LocalServerPort
    private int port;

    @Test
    public void cors_happy_preflight() throws IOException {

        String host = "http://localhost:"+port;
        String tokenUrl = host+"/auth/oauth/token";

        final CloseableHttpClient client = HttpClients.createDefault();
        HttpOptions request = new HttpOptions(URI.create(tokenUrl));
        request.setHeader( HttpHeaders.ORIGIN, "http://localhost");
        request.setHeader( HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        CloseableHttpResponse response = client.execute(request);
        assertTrue("Expected 2xx got: "+response.getStatusLine(),response.getStatusLine().getStatusCode() < 300);
    }
}
