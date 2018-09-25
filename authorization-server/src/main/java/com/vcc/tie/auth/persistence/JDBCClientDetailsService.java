package com.vcc.tie.auth.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;


@Component
public class JDBCClientDetailsService extends JdbcClientDetailsService {

    public JDBCClientDetailsService(@Autowired DataSource dataSource) {
        super(dataSource);
    }
    Logger logger = LoggerFactory.getLogger(JDBCClientDetailsService.class);
    @Autowired
    @Override
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        super.setPasswordEncoder(passwordEncoder);
    }



}
