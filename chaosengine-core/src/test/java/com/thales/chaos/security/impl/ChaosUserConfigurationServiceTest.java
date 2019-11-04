package com.thales.chaos.security.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "holidays=DUM", "chaos.security.users[0].username=" + ChaosUserConfigurationServiceTest.username, "chaos.security.users[0].password=" + ChaosUserConfigurationServiceTest.password, "chaos.security.users[0].authorities=ADMIN" })
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ChaosUserConfigurationServiceTest {
    public static final String username = "admin";
    public static final String password = "password";
    @Autowired
    private MockMvc mvc;

    @Test
    public void testSuccessfulLogin () throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                                  .param("username", username)
                                  .param("password", password))
           .andExpect(status().isOk())
           .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", instanceOf(SecurityContext.class)));
    }

    @Test
    public void testFailedLogin () throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                                  .param("username", username)
                                  .param("password", password + " ")).andExpect(status().isNotFound());
    }

    @Test
    @WithAdmin
    public void testRepeatedLogin () throws Exception {
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
                                  .param("username", username)
                                  .param("password", password))
           .andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", instanceOf(SecurityContext.class)))
           .andExpect(status().isOk());
    }

    @Test
    @WithAdmin
    public void authenticatedLogoutTest () throws Exception {
        mvc.perform(post("/logout")).andExpect(redirectedUrl("/login?logout"));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @WithMockUser(username = username, roles = "ADMIN")
    private @interface WithAdmin {
    }
}
