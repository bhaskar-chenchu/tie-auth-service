package com.vcc.tie.auth;

import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.Charset;

import static org.junit.Assert.*;

@ActiveProfiles("unittest")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Oauth2AuthorizationFlowsTest {

  @Autowired private RestTemplateBuilder restTemplate;

  @LocalServerPort private int port;

  @Test
  public void unauthenticated_user_can_access_public_cert_endpoint() {
    RequestEntity requestEntity =
        RequestEntity.get(
                UriComponentsBuilder.fromHttpUrl("http://localhost")
                    .port(port)
                    .path("/auth/oauth/token_key")
                    .build()
                    .toUri())
            .build();
    ResponseEntity<String> responseEntity =
        restTemplate.build().exchange(requestEntity, String.class);
    assertTrue(responseEntity.getStatusCode().is2xxSuccessful());
    assertNotNull(responseEntity.getBody());
    String dsa = responseEntity.getBody();
    System.out.println(dsa);
  }

  @Test
  public void get_token_with_password_grant() {
    String username = "test_user";
    String password = "Pass1234";
    String clientId = "test_ui_client_id";
    String clientPassword = "Pass1234";
    RequestEntity tokenRequest =
        RequestEntity.post(
                UriComponentsBuilder.fromHttpUrl("http://localhost")
                    .port(port)
                    .path("/auth")
                    .path("/oauth")
                    .path("/token")
                    .queryParam("grant_type", "password")
                    .queryParam("username", username)
                    .queryParam("password", password)
                    .queryParam("client_id", clientId)
                    .build()
                    .toUri())
            .header("Authorization", createBasicAuthHeader(clientId, clientPassword))
            .build();
    BaseClientDetails details = restTemplate.build().exchange(tokenRequest, BaseClientDetails.class).getBody();
    assertNotNull(details.getAdditionalInformation().get("access_token").toString());
    assertNotNull(details.getAdditionalInformation().get("partnerid").toString());
  }

  @Test(expected = HttpClientErrorException.class)
  public void get_token_with_password_grant_throws_HttpClientErrorException_on_bad_password() {

    String username = "test_user";
    String password = "incorrect-password";
    String clientId = "test_ui_client_id";
    String clientPassword = "Pass1234";
    RequestEntity tokenRequest =
            RequestEntity.post(
                    UriComponentsBuilder.fromHttpUrl("http://localhost")
                            .port(port)
                            .path("/auth")
                            .path("/oauth")
                            .path("/token")
                            .queryParam("grant_type", "password")
                            .queryParam("username", username)
                            .queryParam("password", password)
                            .queryParam("client_id", clientId)
                            .build()
                            .toUri())
                    .header("Authorization", createBasicAuthHeader(clientId, clientPassword))
                    .build();
    BaseClientDetails details = restTemplate.build().exchange(tokenRequest, BaseClientDetails.class).getBody();
    assertNotNull(details.getAdditionalInformation().get("access_token").toString());
    assertNotNull(details.getAdditionalInformation().get("partnerid").toString());
  }
  /** Ignored - since we will use another teams auth server */
  @Ignore
  @Test
  public void get_token_with_authorization_code_grant() {
    String username = "test_user";
    String password = "Pass1234";
    String clientId = "test_ui_client_id";
    String clientPassword = password;
    fail("not impl");
  }

  String createBasicAuthHeader(String username, String password) {
    String auth = username + ":" + password;
    byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
    return "Basic " + new String(encodedAuth);
  }
}
