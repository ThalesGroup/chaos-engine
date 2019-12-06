package com.thales.chaos.security.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thales.chaos.security.UserConfigurationService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;

@Configuration
@ConfigurationProperties("chaos.security")
public class ChaosUserConfigurationService implements UserConfigurationService {
    private Collection<User> users;

    @Override
    @JsonIgnore
    public Collection<User> getUsers () {
        return users;
    }

    public void setUsers (Collection<User> users) {
        this.users = users;
    }
}
