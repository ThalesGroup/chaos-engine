package com.gemalto.chaos.admin;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.admin.enums.AdminState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @GetMapping("/state")
    public AdminState getAdminState () {
        return AdminManager.getAdminState();
    }

    @PostMapping("/state")
    public void setAdminState (@RequestParam("state") String newAdminStateString) {
        AdminState newAdminState;
        newAdminStateString = newAdminStateString.toUpperCase();
        log.info("Setting admin state to {}", newAdminStateString);
        try {
            newAdminState = AdminState.valueOf(newAdminStateString);
        } catch (IllegalArgumentException e) {
            log.error("Tried to set an invalid state", e);
            throw new ChaosException("Tried to set an invalid state", e);
        }
        AdminManager.setAdminState(newAdminState);
    }
}
