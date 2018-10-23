package com.vcc.tie.sample.rest;

import com.vcc.tie.sample.domain.SomeService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.token.TokenService;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;


@RestController
@RequestMapping("secured")
public class SomeController {

    @Autowired
    ResourceServerTokenServices dsad;

    @Autowired
    SomeService someService;
/*
    this.technicianRole = InMemoryRole.builder()
                .name("TECHNICIAN")
                .privilege(new InMemoryPrivilage("some-techincal-action"))
                .build();
        this.marketAdmin = InMemoryRole.builder()
                .name("MARKET_ADMIN")
                .privilege(new InMemoryPrivilage("add-workshop"))
                .privilege(new InMemoryPrivilage("remove-workshop"))
                .build();
    }
 */



    @PreAuthorize("hasAuthority('add-workshop') OR hasAuthority('${security.configurable.authority}')")
    @RequestMapping(path = "/workshops/", method = RequestMethod.POST)
    public String addWorkShop(){
        return "workshop added";
    }

    @PreAuthorize("hasRole('TECHNICIAN') OR hasAuthority('some-techincal-action')")
    @RequestMapping(path = "/workshops/tech-work/", method = RequestMethod.GET)
    public String performSomeWorkshopRelatedWork(){
        return "some work performed.";
    }


    @PreAuthorize("hasAuthority('remove-workshop')")
    @RequestMapping(path = "/workshops/", method = RequestMethod.DELETE)
    public String removeWorkShop(OAuth2Authentication authentication,
                                 @RequestParam(required = false, name = "workShopIdToRempove", defaultValue = "someUnluckyWorkshopId")  String workshopId  ){
        /* for example purposes I pretend that the marketId has some business purpose when removing a workshop*/
        TieCustomClaims tieCustomClaims = extractTieCustomClaimsFromToken(authentication);
        someService.removeWorkShop(
                tieCustomClaims.getMarketId().orElseThrow(() -> new IllegalStateException("user is not associated with a market"))
                ,workshopId);
       return "some friendly message";
    }

    private TieCustomClaims extractTieCustomClaimsFromToken(OAuth2Authentication authentication){
        OAuth2AccessToken accessToken = dsad.readAccessToken(((OAuth2AuthenticationDetails)authentication.getDetails()).getTokenValue());
        return TieCustomClaims.builder()
                .marketId(asStringOrNull(accessToken.getAdditionalInformation().get("marketId")))
                .partnerId(asStringOrNull(accessToken.getAdditionalInformation().get("partnerid")))
                .workShopId(asStringOrNull(accessToken.getAdditionalInformation().get("workshop?Id")))
                .build();
    }

    private String asStringOrNull(Object o){
        return o != null ? o.toString() : null;
    }


    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Builder
    private static class TieCustomClaims{
        private String marketId;
        private String workShopId;
        private String partnerId;

        public Optional<String> getMarketId() {
            return Optional.ofNullable(marketId);
        }

        public Optional<String> getWorkShopId() {
            return Optional.ofNullable(workShopId);
        }

        public Optional<String> getPartnerId() {
            return Optional.ofNullable(partnerId);
        }
    }

}
