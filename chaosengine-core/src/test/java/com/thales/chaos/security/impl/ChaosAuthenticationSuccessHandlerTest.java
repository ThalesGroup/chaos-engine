/*
 *    Copyright (c) 2018 - 2020, Thales DIS CPL Canada, Inc
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.thales.chaos.security.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class ChaosAuthenticationSuccessHandlerTest {
    @MockBean
    private HttpSessionRequestCache httpSessionRequestCache;
    @Autowired
    private ChaosAuthenticationSuccessHandler chaosAuthenticationSuccessHandler;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication exception;
    @Mock
    private HttpSession session;
    @Mock
    private SavedRequest savedRequest;

    @Before
    public void setUp () {
        doReturn(session).when(request).getSession(anyBoolean());
        doReturn(savedRequest).when(httpSessionRequestCache).getRequest(request, response);
    }

    @Test
    public void onAuthenticationSuccessWithNullSession () {
        doReturn(null).when(httpSessionRequestCache).getRequest(request, response);
        chaosAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, exception);
        verify(httpSessionRequestCache, never()).removeRequest(request, response);
    }

    @Test
    public void onAuthenticationSuccessWithTargetedUrl () {
        chaosAuthenticationSuccessHandler.setAlwaysUseDefaultTargetUrl(true);
        chaosAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, exception);
        verify(httpSessionRequestCache, times(1)).removeRequest(request, response);
    }

    @Test
    public void onAuthenticationSuccessWithNullTargetUrl () {
        chaosAuthenticationSuccessHandler.setAlwaysUseDefaultTargetUrl(false);
        chaosAuthenticationSuccessHandler.setTargetUrlParameter(null);
        chaosAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, exception);
        verify(httpSessionRequestCache, never()).removeRequest(request, response);
    }

    @Test
    public void onAuthenticationSuccessWithEmptyTargetUrl () {
        chaosAuthenticationSuccessHandler.setAlwaysUseDefaultTargetUrl(false);
        // Using ReflectionTestUtils to set the field since the setter has an empty-check
        ReflectionTestUtils.setField(chaosAuthenticationSuccessHandler, "targetUrlParameter", "");
        chaosAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, exception);
        verify(httpSessionRequestCache, never()).removeRequest(request, response);
    }

    @Test
    public void onAuthenticationSuccessWithTargetUrlParameter () {
        chaosAuthenticationSuccessHandler.setAlwaysUseDefaultTargetUrl(false);
        chaosAuthenticationSuccessHandler.setTargetUrlParameter("/not empty!");
        chaosAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, exception);
        verify(httpSessionRequestCache, times(1)).removeRequest(request, response);
    }

    @After
    public void tearDown () {
        verify(session).removeAttribute("SPRING_SECURITY_LAST_EXCEPTION");
    }

    @Configuration
    static class ContextConfiguration {
        @Autowired
        private HttpSessionRequestCache httpSessionRequestCache;

        @Bean
        public ChaosAuthenticationSuccessHandler chaosAuthenticationSuccessHandler () {
            return spy(new ChaosAuthenticationSuccessHandler(httpSessionRequestCache));
        }
    }
}