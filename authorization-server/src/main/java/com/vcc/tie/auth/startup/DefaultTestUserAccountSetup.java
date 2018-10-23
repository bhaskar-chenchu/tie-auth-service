package com.vcc.tie.auth.startup;


import com.vcc.tie.auth.claims.TieClaimsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Collections;

@Service
public class DefaultTestUserAccountSetup {

    Logger logger = LoggerFactory.getLogger(DefaultTestUserAccountSetup.class);
    private final JdbcUserDetailsManager jdbcUserDetailsManager;
    private final PasswordEncoder passwordEncoder;
    private final TieClaimsProvider tieClaimsProvider;

    public DefaultTestUserAccountSetup(DataSource dataSource, PasswordEncoder passwordEncoder, TieClaimsProvider tieClaimsProvider){
        this.jdbcUserDetailsManager = new JdbcUserDetailsManager(dataSource);
        this.passwordEncoder = passwordEncoder;
        this.tieClaimsProvider = tieClaimsProvider;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createSystemAccounts() {
        createTechinicanTetsUser();
        createMarketAdminTestUser();
    }

    private void createMarketAdminTestUser() {
        UserDetails userDetails = createTestUser("test_market_user");
        tieClaimsProvider.registerAsMarketAdmin(userDetails.getUsername());
        store(userDetails);
    }

    private void createTechinicanTetsUser() {
        UserDetails userDetails = createTestUser("test_user");
        tieClaimsProvider.registerAsTechnician(userDetails.getUsername());
        store(userDetails);
    }

    private void store(UserDetails userDetails) {
        try{
            jdbcUserDetailsManager.createUser(userDetails);
            logger.info("Created account for {} with password {} ", userDetails.getUsername(), userDetails.getPassword());
        }
        catch (DuplicateKeyException e){
            jdbcUserDetailsManager.updateUser(userDetails);
            logger.info("Updated account for {} with password {}", userDetails.getUsername(), userDetails.getPassword());
        }

    }

    private UserDetails createTestUser(String username) {

       return User.builder()
                .passwordEncoder(passwordEncoder::encode)
                .username(username)
                .password("Pass1234")
                .authorities(Collections.emptySet())
                .roles("somerole").build();

    }


}
