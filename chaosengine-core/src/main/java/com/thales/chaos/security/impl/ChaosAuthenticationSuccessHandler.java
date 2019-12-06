package com.thales.chaos.security.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ChaosAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private RequestCache requestCache;

    @Autowired
    public ChaosAuthenticationSuccessHandler () {
        this.requestCache = new HttpSessionRequestCache();
    }

    ChaosAuthenticationSuccessHandler (HttpSessionRequestCache requestCache) {
        this.requestCache = requestCache;
    }

    @Override
    public void onAuthenticationSuccess (HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        ChaosWebSecurity.logSuccessfulRequest("Authentication success", httpServletRequest);
        SavedRequest request = requestCache.getRequest(httpServletRequest, httpServletResponse);
        if (request == null) {
            clearAuthenticationAttributes(httpServletRequest);
            return;
        }
        String targetUrlParameter = getTargetUrlParameter();
        if (isAlwaysUseDefaultTargetUrl() || (targetUrlParameter != null && !targetUrlParameter.isEmpty())) {
            requestCache.removeRequest(httpServletRequest, httpServletResponse);
            clearAuthenticationAttributes(httpServletRequest);
            return;
        }
        clearAuthenticationAttributes(httpServletRequest);
    }
}
