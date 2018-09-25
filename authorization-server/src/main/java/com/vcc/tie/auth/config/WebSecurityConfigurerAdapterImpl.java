package com.vcc.tie.auth.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;

@EnableGlobalMethodSecurity
@Order(1)
@Configuration
public class WebSecurityConfigurerAdapterImpl extends WebSecurityConfigurerAdapter {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
  /*
  @Autowired
  private CorsConfigurationSource corsConfigurationSource;*/

  @Autowired private DataSource dataSource;

  @Autowired private PasswordEncoder passwordEncoder;

  @Bean
  public AuthenticationManager authenticationManagerBean() throws Exception {
    AuthenticationManager mgr = super.authenticationManagerBean();
    return mgr;
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.jdbcAuthentication().dataSource(dataSource).passwordEncoder(passwordEncoder);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    http.requestMatchers()
        .antMatchers(HttpMethod.OPTIONS, "/oauth/token")
        .and()
        .cors()
        .disable()
        .csrf()
        .disable();
    http.authorizeRequests().antMatchers("/**").permitAll();
  }
}
