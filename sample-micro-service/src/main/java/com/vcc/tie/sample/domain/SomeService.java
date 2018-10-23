package com.vcc.tie.sample.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class SomeService {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @PreAuthorize("hasRole(dsa)")
    public void doStuff(){
        logger.info("doing stuff..");
    }

    public void removeWorkShop(String marketId, String workshopid) {
        /* maybe this would only remove the workshop if the workshop belonged to the market or something
         */
    }
}
