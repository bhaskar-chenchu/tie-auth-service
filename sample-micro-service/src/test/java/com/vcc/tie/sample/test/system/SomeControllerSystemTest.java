package com.vcc.tie.sample.test.system;

import com.vcc.tie.sample.util.JwtTokenClient;
import org.apache.tomcat.util.codec.binary.Base64;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.Charset;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * In oauth the access token is what identifies you, it also carries what authorities your client
 * has client is the rather confusing name the system that requested access on behalf of the user,
 * the granularity is not defined. We might decide to have all microservices represented as the same
 * client f.ex. Client has NOTHING to do with the end-user (the human) It would not make sense to
 * let your Iphone access your bank-account f.ex, while you would trust your PC to do it so same
 * user but two different clients :). The permissions that a particular client (like your iphone or
 * PC) has is defined as SCOPES in the oauth2 protocol.
 *
 * <p>A client will also have different methods of authenticating a user (these are called GRANTS).
 * Again you obviously would not want to give your password to an random application in the Iphone
 * (because you can not trust the app), however you would probably be fine with the Iphone using
 * chrome towards a server and send your password.
 *
 * <p>The last part of the token is the things representing the end-user, by default this is only
 * the user_name (considered public information security wise) However, the oAuth2 token is
 * extensible with whatever attributes you want, such non-standard attributes do not fill any
 * function in the Oauth2 specification, but they are allowed. JWT f.ex is just a few custom
 * attributes added to the oauth token to make stateless systems easier to build. One of the thing
 * spring adds as its own non-standard attribute of the token is the authorities attribute, spring
 * uses this so it can seemlessly (lol) integrate with the existing spring security standards to
 * transport ROLES and PRIVILEGES with the JWT token. This is what allows us to use the
 * fancy @Secured, @RolesAllowed and @PreAuthorized annotations. However since any attribute can be
 * added to the token (this is usually called adding a "claim"), it is not uncommon to add stuff
 * like the organization of a user to it, as long as the information is considered non-sensitive.
 *
 * <p>Another party that likes adding claims to the token is Google, who have standardised a set of
 * useful claims in the OpenId specification.
 *
 * <p>For the TIE team we intend to add the marketId, the partnerId and the workshopId, because all
 * of them will save us a lot of needless calls and none of them are sensitive.
 *
 * <p>A much more detailed explanation was written by someone at volvo and can be found at:
 * https://sharepoint.volvocars.net/sites/cits/CCIS/operations/volvoid/Shared%20Documents/OpenID/Connected_Car_Authentication_Specification.pdf
 * The document describes OpenId witch is built on top of oauth2 by adding custom claims.
 *
 * <p>This test requires an active authorization server to run, hence it is similar from a
 * deployment perspective to a system test, albeit with a limited subset of services. Personally I
 * do not like this test-setup because of its inflexibility and weight, the gain is that the
 * communication with a real authorization server is carried out. Since we are using spring security
 * on both sides testing their code is not really sensible.
 */
@ActiveProfiles("dev")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SomeControllerSystemTest {

  @Autowired private RestTemplateBuilder restTemplate;

  @LocalServerPort private int port;

  @Value("${security.oauth2.resource.host}")
  private String authorizationServerUrl;

  @Autowired private JwtTokenClient jwtTokenClient;

  /* Since the public key is not sensitive it should be publicly accessible so
  services can obtain something to validate the JWT with ease.
  */
  @Test
  public void unauthenticated_user_can_access_public_key_endpoint() {
    RequestEntity requestEntity =
        RequestEntity.get(
                UriComponentsBuilder.fromHttpUrl(authorizationServerUrl)
                    .path("/token_key")
                    .build()
                    .toUri())
            .build();
    ResponseEntity<String> responseEntity =
        restTemplate.build().exchange(requestEntity, String.class);
    String publicKey = responseEntity.getBody();
  }

  /**
   * This is essentially saying, "as an application I want to exchange this users credentials for an
   * access-token". The application will also be authenticated (the standard does however not
   * mandate that), I load those parameters in the jwtToken client.
   *
   * <p>The user account and the client account was created as part of the Authorization modules
   * startup process, if it was a production ready authorization server there would be some UI to
   * add new clients (applications), some process to approve the clients for usage (like a human
   * doing something). End users on the other hand would likely have a signup page, possibly with
   * some approval process for them as well. Since this is just for demo purposes those things have
   * been skipped (instead startup scripts does it).
   */
  @Test
  public void get_token_with_password_grant() {
    JwtTokenClient.JwtTokenResponse tokenResponse =
        jwtTokenClient.requestTokenWithPasswordGrant("test_user", "Pass1234");
    assertNotNull(tokenResponse.getAccessToken());
    assertNotNull(tokenResponse.getRefreshToken());
  }

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
                .path("/crud")
                .path("/workshops")
                .build()
                .toUri(),
            String.class);
  }

  @Test
  public void access_protected_resource_with_token_as_technician() {

    String username = "test_user";
    String password = "Pass1234";
    JwtTokenClient.JwtTokenResponse tokenResponse = requestToken(username, password);
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

    String username = "test_market_user";
    String password = "Pass1234";
    JwtTokenClient.JwtTokenResponse tokenResponse = requestToken(username, password);
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

  @Test
  public void access_protected_resource_with_token_as_market_user_and_use_the_marketId_claim() {

    String username = "test_market_user";
    String password = "Pass1234";
    JwtTokenClient.JwtTokenResponse tokenResponse = requestToken(username, password);
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

  private JwtTokenClient.JwtTokenResponse requestToken(String username, String password) {
    return jwtTokenClient.requestTokenWithPasswordGrant(username, password);
  }

  private String createBearerTokenHeader(Optional<OAuth2AccessToken> accessToken) {
    return accessToken
        .map(t -> "Bearer " + t.getValue())
        .orElseThrow(() -> new IllegalStateException());
  }

  /**
   * Ignored - since we will use another teams auth server and its a bit of effort to implement the
   * full authorization_code grant
   */
  @Ignore
  @Test
  public void get_token_with_authorization_code_grant() {
    fail("not impl");
  }
}
