package com.thales.chaos.security.impl;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ChaosAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence (HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException {
        ChaosWebSecurity.logUnsuccessfulRequest("Unauthorized request", httpServletRequest);
        httpServletResponse.sendError(HttpStatus.NOT_FOUND.value(), HttpStatus.NOT_FOUND.getReasonPhrase());
    }
}
