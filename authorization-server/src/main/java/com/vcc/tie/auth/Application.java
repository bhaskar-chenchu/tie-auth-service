package com.vcc.tie.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import java.security.Principal;


@SpringBootApplication
@RestController
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws Throwable {
        SpringApplication.run(Application.class, args);
    }


    @RequestMapping("/user")
    public Principal user(Principal user) {
        logger.info("AS /user has been called");
        logger.debug("user info: "+user.toString());
        return user;
    }

    @Bean
    public CommonsRequestLoggingFilter logFilter() {

        CommonsRequestLoggingFilter filter
                = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(true);
        filter.setAfterMessagePrefix("REQUEST DATA : ");
        return filter;
    }

}