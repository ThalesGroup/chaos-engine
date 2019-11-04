package com.thales.chaos.security.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class ChaosAuthenticationEntryPointTest {
    @Autowired
    private ChaosAuthenticationEntryPoint chaosAuthenticationEntryPoint;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private AuthenticationException exception;

    @Test
    public void commence () throws Exception {
        chaosAuthenticationEntryPoint.commence(request, response, exception);
        verify(response).sendError(404, "Not Found");
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        public ChaosAuthenticationEntryPoint chaosAuthenticationEntryPoint () {
            return spy(new ChaosAuthenticationEntryPoint());
        }
    }
}