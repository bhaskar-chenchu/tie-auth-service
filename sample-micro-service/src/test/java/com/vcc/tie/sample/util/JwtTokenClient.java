package com.vcc.tie.sample.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.jwt.crypto.sign.SignatureVerifier;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Component
public class JwtTokenClient {

  private final ObjectMapper objectMapper;
  private final CloseableHttpClient client = HttpClients.createDefault();
  private final String oauthUrl;
  private final String clientId;
  private final String clientSecret;
  private final JwtTokenStore jwtTokenStore;

  public JwtTokenClient(
      @Value("${security.oauth2.resource.host: http://localhost:9000/auth/oauth}") String url,
      @Value("${app.client.id: test_ui_client_id}") String clientId,
      @Value("${app.client.secret: Pass1234}") String clientSecret) {

    this.objectMapper = new ObjectMapper();
    this.oauthUrl = url.trim();
    this.clientId = clientId.trim();
    this.clientSecret = clientSecret.trim();
    JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
    jwtAccessTokenConverter.setVerifier(createNoopSignatureVerifier());
    this.jwtTokenStore = new JwtTokenStore(jwtAccessTokenConverter);
  }

  /**
   * Since spring security would verify the signature anyway in normal operations there is not much
   * point doing it here (the tokenstore is not explicitly exposed as a bean and cba to expose it
   * just for testing)
   *
   * @return
   */
  private SignatureVerifier createNoopSignatureVerifier() {
    return new SignatureVerifier() {
      @Override
      public void verify(byte[] bytes, byte[] bytes1) {}

      @Override
      public String algorithm() {
        return null;
      }
    };
  }

  public JwtTokenResponse requestTokenWithPasswordGrant(String username, String clearTextPassword) {

    try {
      HttpPost httpPost =
          new HttpPost(
              UriComponentsBuilder.fromHttpUrl(oauthUrl)
                  .path("/token")
                  .queryParam("grant_type", "password")
                  .queryParam("username", username)
                  .queryParam("password", clearTextPassword)
                  .queryParam("client_id", clientId)
                  .build()
                  .toUri());
      httpPost.setHeader("Authorization", createBasicAuthHeader(clientId, clientSecret));
      CloseableHttpResponse response = client.execute(httpPost);
      if (response.getStatusLine().getStatusCode() >= 400) {
        throw new IllegalStateException(response.getStatusLine().toString());
      }
      return readInputSteam(response.getEntity().getContent());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private JwtTokenResponse readInputSteam(InputStream inputStream) {
    try {
      Map<String, Object> map = objectMapper.readValue(inputStream, Map.class);
      return JwtTokenResponse.builder()
          .accessToken(readNonNull(jwtTokenStore::readAccessToken, map.get("access_token")))
          .refreshToken(readNonNull(jwtTokenStore::readRefreshToken, map.get("refresh_token")))
          .build();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private <T> T readNonNull(Function<String, T> func, Object arg) {
    if (arg == null) {
      return null;
    }
    return func.apply(arg.toString());
  }

  public JwtTokenResponse requestTokenWithClientCredentialsGrant() {
    throw new UnsupportedOperationException("not impl");
  }

  public JwtTokenResponse requestTokenWithRefreshTokenGrant() {
    throw new UnsupportedOperationException("not impl");
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Builder
  public static class JwtTokenResponse {

    private OAuth2AccessToken accessToken;
    private OAuth2RefreshToken refreshToken;

    public Optional<OAuth2AccessToken> getAccessToken() {
      return Optional.ofNullable(accessToken);
    }

    public Optional<OAuth2RefreshToken> getRefreshToken() {
      return Optional.ofNullable(refreshToken);
    }
  }

  String createBasicAuthHeader(String username, String password) {
    String auth = username + ":" + password;
    byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
    return "Basic " + new String(encodedAuth);
  }
}
