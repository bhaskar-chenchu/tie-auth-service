package com.vcc.tie.auth.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientAlreadyExistsException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Profile({"unittest", "localhost","docker-localhost"})
@Service
public class DefaultTestClientAccountSetup {

    Logger logger = LoggerFactory.getLogger(DefaultTestClientAccountSetup.class);


    private JdbcClientDetailsService clientDetailsService;



    public DefaultTestClientAccountSetup(DataSource ds, PasswordEncoder encoder){
        clientDetailsService = new JdbcClientDetailsService(ds);
        clientDetailsService.setPasswordEncoder(encoder);

    }


    @EventListener(ApplicationReadyEvent.class)
    public void createSystemAccounts() {


        addOrUpdate(createTestUIClient());

    }

    private BaseClientDetails createTestVccClient(){
        BaseClientDetails clientDetails = new BaseClientDetails();
        clientDetails.setClientSecret("Pass1234");
        clientDetails.setClientId("test_ui_client_id");

        clientDetails.setAuthorizedGrantTypes(Arrays.asList("client_credentials"));
        //  clientDetails.setRegisteredRedirectUri(Arrays.asList("http://localhost:8080/").stream().collect(Collectors.toSet()));
        clientDetails.setScope(Arrays.asList("tie:partner:admin", "tie:market:admin"));
        Map<String, String> additonal = new HashMap<>();
        additonal.put("dsa", UUID.randomUUID().toString());
        clientDetails.setAdditionalInformation(additonal);
        return clientDetails;
    }

    private BaseClientDetails createTestUIClient() {
        BaseClientDetails clientDetails = new BaseClientDetails();
        clientDetails.setClientSecret("Pass1234");
        clientDetails.setClientId("test_ui_client_id");

        clientDetails.setAuthorizedGrantTypes(Arrays.asList("client_credentials", "password","refresh_token","authorization_code"));
      //  clientDetails.setRegisteredRedirectUri(Arrays.asList("http://localhost:8080/").stream().collect(Collectors.toSet()));
        clientDetails.setScope(Arrays.asList("tie:read", "tie:write"));
        Map<String, String> additonal = new HashMap<>();
        additonal.put("dsa", UUID.randomUUID().toString());
        clientDetails.setAdditionalInformation(additonal);
        return clientDetails;
    }

    private void addOrUpdate(BaseClientDetails clientDetails2) {
        try{
            clientDetailsService.addClientDetails(clientDetails2);
            logger.info("Created account for {} with password {} ", clientDetails2.getClientId(), clientDetails2.getClientSecret());
        }
        catch (ClientAlreadyExistsException e){
            clientDetailsService. updateClientDetails(clientDetails2);
            logger.info("Updated account for {} with password {}", clientDetails2.getClientId(), clientDetails2.getClientSecret());
        }
    }
}
