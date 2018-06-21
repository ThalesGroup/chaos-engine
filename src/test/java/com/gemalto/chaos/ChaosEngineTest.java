package com.gemalto.chaos;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

@RunWith(SpringRunner.class)
public class ChaosEngineTest {

    @Test
    public void cloudFoundryContextLoads () {
        HashMap<String, String> cloudFoundryProperties = new HashMap<>();
        cloudFoundryProperties.put("cf_apihost", "localhost");
        cloudFoundryProperties.put("cf_username", "username");
        cloudFoundryProperties.put("cf_password", "password");
        cloudFoundryProperties.put("cf_organization", "organization");
        cloudFoundryProperties.forEach(System::setProperty);
        ChaosEngine.main(new String[]{});
        cloudFoundryProperties.keySet().forEach(System::clearProperty);
    }
}