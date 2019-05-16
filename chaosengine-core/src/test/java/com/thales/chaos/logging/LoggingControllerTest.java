package com.thales.chaos.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.core.Is.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(LoggingController.class)
public class LoggingControllerTest {
    private static final String OTHER_CLASS = "org.nosuchclass";
    private static final String GEMALTO_CLASS = "com.thales";
    private static final Logger GEMALTO_LOGGER = (Logger) LoggerFactory.getLogger(GEMALTO_CLASS);
    private static final Logger OTHER_LOGGER = (Logger) LoggerFactory.getLogger(OTHER_CLASS);
    @Autowired
    private MockMvc mvc;

    @Test
    public void loggingController () throws Exception {
        mvc.perform(post("/logging/" + OTHER_CLASS).contentType(APPLICATION_JSON).param("loggingLevel", "DEBUG"))
           .andExpect(status().isOk());
        mvc.perform(get("/logging/" + OTHER_CLASS).contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", is("DEBUG")));
        mvc.perform(post("/logging/").contentType(APPLICATION_JSON).param("loggingLevel", "INFO"))
           .andExpect(status().isOk());
        mvc.perform(get("/logging/" + GEMALTO_CLASS).contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", is("INFO")));
        mvc.perform(get("/logging").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", is("INFO")));
        mvc.perform(post("/logging/").contentType(APPLICATION_JSON).param("loggingLevel", "WARN"))
           .andExpect(status().isOk());
        mvc.perform(get("/logging/" + GEMALTO_CLASS).contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", is("WARN")));
        mvc.perform(get("/logging").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", is("WARN")));
    }

    @After
    public void tearDown () {
        GEMALTO_LOGGER.setLevel(Level.OFF);
        OTHER_LOGGER.setLevel(Level.OFF);
    }
}