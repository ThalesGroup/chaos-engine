package com.thales.chaos.logging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(LoggingController.class)
public class LoggingControllerTest {
    private static final String FIVE_MINUTES = Duration.ofMinutes(5).toString();
    @Autowired
    private MockMvc mvc;
    @MockBean
    private LoggingManager loggingManager;

    @Before
    public void setUp () {
        doCallRealMethod().when(loggingManager).setDebugMode();
    }

    @Test
    public void setDebugModeDefaultParameters () throws Exception {
        mvc.perform(post("/logging/").contentType(APPLICATION_JSON))
           .andExpect(status().isOk());
        verify(loggingManager, times(1)).setDebugMode();
        verify(loggingManager, times(1)).setDebugMode(Duration.ofMinutes(30));
    }

    @Test
    public void setDebugModeForFiveMinutes () throws Exception {
        mvc.perform(post("/logging/" + FIVE_MINUTES).contentType(APPLICATION_JSON)).andExpect(status().isOk());
        verify(loggingManager, times(1)).setDebugMode(Duration.ofMinutes(5));
    }

    @Test
    public void clearDebugMode () throws Exception {
        mvc.perform(delete("/logging/").contentType(APPLICATION_JSON)).andExpect(status().isOk());
        verify(loggingManager, times(1)).clearDebugMode();
    }
}