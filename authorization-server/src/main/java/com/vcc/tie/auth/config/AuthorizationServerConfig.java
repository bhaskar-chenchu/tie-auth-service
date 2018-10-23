package com.vcc.tie.auth.config;

import com.vcc.tie.auth.claims.TieClaimsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import javax.sql.DataSource;
import java.util.*;


@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {




    @Autowired
    private Environment environment;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TieClaimsProvider tieClaimsProvider;


    @Autowired
    private DataSource dataSource;

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {

        clients.jdbc(dataSource);
    }

    /**
     * Configure the {@link ClientDetailsService}, e.g. declaring individual clients and their properties. Note that
     * password grant is not enabled (even if some clients are allowed it) unless an {@link AuthenticationManager} is
     * supplied to the {@link #configure(AuthorizationServerEndpointsConfigurer)}. At least one client, or a fully
     * formed custom {@link ClientDetailsService} must be declared or the server will not start.
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {

        TokenEnhancerChain chain = new TokenEnhancerChain();
        chain.setTokenEnhancers(Arrays.asList(createTieTokenEnhancer(), jwtAccessTokenConverter()));
        endpoints.tokenStore(tokenStore())
                .tokenEnhancer(chain)
                .allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS)
                .authenticationManager(authenticationManager);
    }


    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {

        security.tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()");

    }

    @Bean
    public TokenStore tokenStore() {

        return new JwtTokenStore(jwtAccessTokenConverter());

    }

    private TokenEnhancer createTieTokenEnhancer() {
        return new TieTokenEnhancer(this.tieClaimsProvider);
    }

    private static class TieTokenEnhancer implements TokenEnhancer {


        private final TieClaimsProvider tieClaimsProvider;

        private TieTokenEnhancer(TieClaimsProvider tieClaimsProvider) {
            this.tieClaimsProvider = tieClaimsProvider;
        }

        @Override
        public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
            Map<String, Object> additionalInfo = (accessToken.getAdditionalInformation() != null)
                    ? new HashMap<>(accessToken.getAdditionalInformation())
                    : new HashMap<>();
            additionalInfo.put("partnerid", UUID.randomUUID().toString());
            additionalInfo.put("marketId", UUID.randomUUID().toString());
            additionalInfo.put("workshopId", UUID.randomUUID().toString());

            Set<String> authorities = new TreeSet<>();


            authorities.addAll(tieClaimsProvider.getUserPrivileges(authentication.getUserAuthentication().getName()));
            tieClaimsProvider.getUserRoles(authentication.getName()).map(s -> "ROLE_" + s)
                    .ifPresent(authorities::add);

            additionalInfo.put("authorities", authorities);

            ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(additionalInfo);
            return accessToken;
        }
    }

    @Bean
    protected JwtAccessTokenConverter jwtAccessTokenConverter() {
        String pwd = environment.getProperty("keystore.password");
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(
                new ClassPathResource("jwt.jks"),
                pwd.toCharArray());
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
        converter.setKeyPair(keyStoreKeyFactory.getKeyPair("jwt"));
        return converter;
    }
}