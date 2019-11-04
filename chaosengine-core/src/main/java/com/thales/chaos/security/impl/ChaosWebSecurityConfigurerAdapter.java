package com.thales.chaos.security.impl;

import com.thales.chaos.security.UserConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.Collection;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class ChaosWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ChaosWebSecurityConfigurerAdapter.class);
    public static final String ADMIN_ROLE = "ADMIN";
    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;
    @Autowired
    private AuthenticationSuccessHandler successHandler;
    @Autowired
    private AuthenticationFailureHandler failureHandler;
    @Autowired
    private UserConfigurationService userConfigurationService;

    @Override
    protected void configure (AuthenticationManagerBuilder auth) throws Exception {
        Collection<UserConfigurationService.User> users = userConfigurationService.getUsers();
        if (users == null || users.isEmpty()) {
            log.warn("System does not have any users configured for REST access. REST API will be unavailable.");
            super.configure(auth);
        } else {
            InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> inMemoryAuth = auth.inMemoryAuthentication();
            users.forEach(user -> inMemoryAuth.withUser(user.getUsername())
                                              .password(encoder().encode(user.getPassword()))
                                              .roles(Optional.of(user)
                                                             .map(UserConfigurationService.User::getRoles)
                                                             .map(s -> s.toArray(new String[]{}))
                                                             .orElse(new String[]{})));
        }
    }

    @Bean
    public PasswordEncoder encoder () {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure (HttpSecurity http) throws Exception {
        http.csrf()
            .disable()
            .exceptionHandling()
            .authenticationEntryPoint(authenticationEntryPoint).and().sessionManagement().maximumSessions(1).and()
            .and().formLogin().successHandler(successHandler).failureHandler(failureHandler)
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
