package com.vcc.tie.sample.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.util.stream.Collectors.toList;

@Component
public class FakeJWTTokenClient {

  @Autowired private ObjectMapper mapper;

  @Getter
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @Builder
  public static class FakeTokenRequest {
    @NonNull @Singular private SortedSet<String> authorities;
    @NonNull @Singular private SortedSet<String> scopes;

    @Builder.Default @NonNull private String workShopId = UUID.randomUUID().toString();
    @Builder.Default @NonNull private String username = UUID.randomUUID().toString();
    @Builder.Default @NonNull private String partnerId = UUID.randomUUID().toString();
    @Builder.Default @NonNull private String clientId = UUID.randomUUID().toString();
    @Builder.Default @NonNull private String marketId = UUID.randomUUID().toString();
  }

  public JwtTokenClient.JwtTokenResponse requestToken(FakeTokenRequest fakeTokenRequest) {
    try {
      String b64Header = createTokenHeader();
      String b64Body = createTokenBody(fakeTokenRequest);
      String b64JwtSignature = createFakeTokenSignature();
      /*The JWT token format is specified as, b64(header).b64(payload).b64(signature)
       * For testing purposes we consider all signatures to be valid
       * */
      OAuth2AccessToken accessToken =
          new DefaultOAuth2AccessToken(
              new StringBuilder()
                  .append(b64Header)
                  .append(".")
                  .append(b64Body)
                  .append(".")
                  .append(b64JwtSignature)
                  .toString());
      return JwtTokenClient.JwtTokenResponse.builder().accessToken(accessToken).build();
    } catch (JsonProcessingException | UnsupportedEncodingException e) {
      throw new IllegalStateException(e);
    }
  }

  private String createFakeTokenSignature() {
    return Base64Utils.encodeToString("totally-fake-signature".getBytes());
  }

  private String createTokenHeader() throws UnsupportedEncodingException, JsonProcessingException {
    Map<String, String> headers = new TreeMap<>();
    headers.put("alg", "RS256");
    headers.put("typ", "JWT");
    return Base64Utils.encodeToString(mapper.writeValueAsString(headers).getBytes("UTF-8"));
  }

  private String createTokenBody(FakeTokenRequest fakeTokenRequest)
      throws UnsupportedEncodingException, JsonProcessingException {
    SortedMap<String, Object> body = new TreeMap<>();
    body.put("authorities", fakeTokenRequest.getAuthorities().stream().collect(toList()));
    body.put("scope", fakeTokenRequest.getScopes().stream().collect(toList()));
    body.put("workshopId", fakeTokenRequest.getWorkShopId());
    body.put("user_name", fakeTokenRequest.getUsername());
    body.put("partnerId", fakeTokenRequest.getPartnerId());
    body.put("client_id", fakeTokenRequest.getClientId());
    body.put("marketId", fakeTokenRequest.getMarketId());
    body.put("exp", Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli() / 1000L);
    body.put("jti", "totally-fake-tokenid");
    return Base64Utils.encodeToString(mapper.writeValueAsString(body).getBytes("UTF-8"));
  }
}
