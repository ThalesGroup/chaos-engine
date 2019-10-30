package com.thales.chaos.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class ChaosWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
    public static final String ADMIN_ROLE = "ADMIN";
    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;
    @Autowired
    private AuthenticationSuccessHandler successHandler;

    @Override
    protected void configure (HttpSecurity http) throws Exception {
        http.csrf()
            .disable()
            .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint)
            .and()
            .formLogin()
            .successHandler(successHandler)
            .loginPage("/login")
            .and()
            .authorizeRequests()
            .antMatchers(HttpMethod.GET)
            .authenticated()
            .anyRequest()
            .hasRole(ADMIN_ROLE)
            .and()
            .logout();
    }

    @Override
    public void configure (WebSecurity web) {
        web.ignoring().antMatchers("/health");
    }
}
