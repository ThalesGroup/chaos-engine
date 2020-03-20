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

package com.thales.chaos.security;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collection;

public interface UserConfigurationService {
    @JsonIgnore
    Collection<User> getUsers ();

    class User {
        private String username;
        private String password;
        private Collection<String> roles;

        public String getUsername () {
            return username;
        }

        public void setUsername (String username) {
            this.username = username;
        }

        @JsonIgnore
        public String getPassword () {
            String tempPassword = password;
            password = null;
            return tempPassword;
        }

        public void setPassword (String password) {
            this.password = password;
        }

        public Collection<String> getRoles () {
            return roles;
        }

        public void setRoles (Collection<String> roles) {
            this.roles = roles;
        }
    }
}
