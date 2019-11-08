package com.thales.chaos.security.impl;

import com.thales.chaos.security.UserConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class ChaosWebSecurity {
    private static final Logger log = LoggerFactory.getLogger(ChaosWebSecurity.class);
    @Configuration
    @ConditionalOnMissingBean(WebSecurityConfigurerAdapter.class)
    public static class ChaosWebSecurityDisabled extends WebSecurityConfigurerAdapter {
        @Autowired
        private AuthenticationSuccessHandler successHandler;
        @Autowired
        private AuthenticationFailureHandler failureHandler;

        @Override
        protected void configure (HttpSecurity http) throws Exception {
            http.csrf()
                .disable()
                .formLogin()
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .loginPage("/login")
                .and()
                .authorizeRequests()
                .anyRequest()
                .permitAll();
        }
    }

    @Configuration
    @ConditionalOnProperty(prefix = "chaos.security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class ChaosWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
        public static final String ADMIN_ROLE = "ADMIN";
        private static final Logger log = LoggerFactory.getLogger(ChaosWebSecurityConfigurerAdapter.class);
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
        public void configure (WebSecurity web) {
            web.ignoring().antMatchers("/health");
        }

        @Override
        protected void configure (HttpSecurity http) throws Exception {
            http.csrf()
                .disable()
                .exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .and()
                .sessionManagement()
                .maximumSessions(1)
                .and()
                .and()
                .formLogin()
                .successHandler(successHandler)
                .failureHandler(failureHandler)
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
    }

    static void logUnsuccessfulRequest (String message, HttpServletRequest request) {
        log.error(message, kv("http.origin", request.getRemoteHost()), kv("http.method", request.getMethod()), kv("http.url", request
                .getRequestURL()), kv("http.useragent", request.getHeader("User-Agent")), kv("http.user", request.getParameter("username")));
    }

    static void logSuccessfulRequest (String message, HttpServletRequest request) {
        log.info(message, kv("http.origin", request.getRemoteHost()), kv("http.method", request.getMethod()), kv("http.url", request
                .getRequestURL()), kv("http.useragent", request.getHeader("User-Agent")), kv("http.user", request.getParameter("username")));
    }
}