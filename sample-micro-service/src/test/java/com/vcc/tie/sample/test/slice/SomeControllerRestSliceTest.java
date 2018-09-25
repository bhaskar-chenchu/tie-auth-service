package com.vcc.tie.sample.test.slice;

import com.vcc.tie.sample.util.FakeJWTTokenClient;
import com.vcc.tie.sample.util.JwtTokenClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.RequestEntity;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Optional;

/**
 * This level of test mocks out the RemoteTokenServices, and as such no validation of the token
 * Authenticity is carried out (its signature becomes irrelevant). However all authorization filters
 * will still apply in the filter chain (hence the security context will be based on the token
 * contents). This allows for any tests against the SecurityContext, such
 * as @PreAuthorize, @RolesAllowed and whatever the third one is. Running a test like this does not
 * require any dependency on an external process, and works quite well with test slicing of the REST
 * layer.
 */
@ActiveProfiles("no-signature-verification")
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties =
        "spring.config.location=classpath:/application-test.yaml") // the autoconfigure for spring
// security is not very flexible,
// and will in particular trigger
// on the
// security.oauth2.resource.jwt.keyUri property
public class SomeControllerRestSliceTest {

  @LocalServerPort private int port;

  @Autowired private FakeJWTTokenClient fakeJWTTokenClient;

  @Autowired private RestTemplateBuilder restTemplate;
  /**
   * This is just confirming that the application is configured correctly, if it is I should be
   * getting a 401 since there is no authentication provided.
   */
  @Test(expected = HttpClientErrorException.class)
  public void access_protected_resource_without_token() {

    restTemplate
        .build()
        .getForEntity(
            UriComponentsBuilder.fromUriString("http://localhost")
                .port(port)
                .path("/secured")
                .path("/workshops")
                .path( "tech-work")
                .build()
                .toUri(),
            String.class);
  }

  @Test
  public void access_protected_resource_with_token_as_technician() {

    JwtTokenClient.JwtTokenResponse tokenResponse =
        createFakeTokenWithAuthorities("ROLE_TECHNICIAN");

    RequestEntity tokenRequest =
        RequestEntity.get(
                UriComponentsBuilder.fromUriString("http://localhost")
                    .port(port)
                    .path("/secured")
                    .path("/workshops")
                    .path("/tech-work/")
                    .build()
                    .toUri())
            .header("Authorization", createBearerTokenHeader(tokenResponse.getAccessToken()))
            .build();
    restTemplate.build().exchange(tokenRequest, String.class).getBody();
  }

  @Test
  public void access_protected_resource_with_token_as_market_user() {

    JwtTokenClient.JwtTokenResponse tokenResponse = createFakeTokenWithAuthorities("add-workshop");
    RequestEntity tokenRequest =
        RequestEntity.post(
                UriComponentsBuilder.fromUriString("http://localhost")
                    .port(port)
                    .path("/secured")
                    .path("/workshops/")
                    .build()
                    .toUri())
            .header("Authorization", createBearerTokenHeader(tokenResponse.getAccessToken()))
            .build();
    restTemplate.build().exchange(tokenRequest, String.class).getBody();
  }

  private JwtTokenClient.JwtTokenResponse createFakeTokenWithAuthorities(String... authorities) {
    return fakeJWTTokenClient.requestToken(
        FakeJWTTokenClient.FakeTokenRequest.builder()
            .authorities(Arrays.asList(authorities))

            .build());
  }

  @Test
  public void access_protected_resource_with_token_as_market_user_and_use_the_marketId_claim() {

    JwtTokenClient.JwtTokenResponse tokenResponse =
        createFakeTokenWithAuthorities("remove-workshop");

    RequestEntity tokenRequest =
        RequestEntity.delete(
                UriComponentsBuilder.fromUriString("http://localhost")
                    .port(port)
                    .path("/secured")
                    .path("/workshops/")
                    .build()
                    .toUri())
            .header("Authorization", createBearerTokenHeader(tokenResponse.getAccessToken()))
            .build();
    restTemplate.build().exchange(tokenRequest, String.class).getBody();
  }

  private String createBearerTokenHeader(Optional<OAuth2AccessToken> accessToken) {
    return accessToken
        .map(t -> "Bearer " + t.getValue())
        .orElseThrow(() -> new IllegalStateException("No access token provided"));
  }
}
