package com.gemalto.chaos.admin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import static com.gemalto.chaos.admin.enums.AdminState.STARTED;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(AdminController.class)
public class AdminControllerTest {
    @Autowired
    private MockMvc mvc;

    @Before
    public void beforeEach () {
        while (AdminManager.getAdminState() != STARTED) {
            AdminManager.setAdminState(STARTED);
            await().untilAsserted(() -> assertSame(STARTED, AdminManager.getAdminState()));
        }
    }

    @Test
    public void getAdminState () throws Exception {
        mvc.perform(get("/admin" + "/state").contentType(APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", is("STARTED")));
    }

    @Test
    public void setAdminState () throws Exception {
        mvc.perform(post("/admin" + "/state").contentType(APPLICATION_JSON).param("state", "BOGUS STATE"))
           .andExpect(status().is5xxServerError());
        mvc.perform(post("/admin" + "/state").contentType(APPLICATION_JSON).param("state", "PAUSED"))
           .andExpect(status().isOk());
    }
}