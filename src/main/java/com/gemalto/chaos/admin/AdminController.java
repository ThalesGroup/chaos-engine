package com.gemalto.chaos.admin;

import com.gemalto.chaos.ChaosException;
import com.gemalto.chaos.admin.enums.AdminState;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    @Autowired
    private AdminManager adminManager;

    @ApiOperation(value = "Get Running State", notes = "Get the current administrative state of the Chaos Engine")
    @GetMapping("/state")
    public AdminState getAdminState () {
        return adminManager.getAdminState();
    }

    @ApiOperation(value = "Set Running State", notes = "Controls whether or not the Chaos Engine will take any action on endpoints. " + "In a STARTED state, all actions can be performed. In a DRAIN state, no new experiments can be created, but existing experiments can still self-heal and finalize. " + "In a PAUSED state, no activity is done.")
    @PostMapping("/state")
    public void setAdminState (@ApiParam(value = "The New Admin State.", required = true, allowableValues = "STARTED, PAUSED, DRAIN") @RequestParam("state") String newAdminStateString) {
        AdminState newAdminState;
        newAdminStateString = newAdminStateString.toUpperCase();
        log.info("Setting admin state to {}", newAdminStateString);
        try {
            newAdminState = AdminState.valueOf(newAdminStateString);
        } catch (IllegalArgumentException e) {
            log.error("Tried to set an invalid state", e);
            throw new ChaosException("Tried to set an invalid state", e);
        }
        adminManager.setAdminState(newAdminState);
    }
}
