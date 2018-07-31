package com.gemalto.chaos;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.HashSet;

@RunWith(SpringJUnit4ClassRunner.class)
public class ChaosEngineTest {
    private HashSet<HashMap<String, String>> systemPropertiesMap;

    @Before
    public void setUp () {
        systemPropertiesMap = new HashSet<>();
        HashMap<String, String> cloudFoundry = new HashMap<>();
        cloudFoundry.put("cf_apihost", "localhost");
        cloudFoundry.put("cf_username", "username");
        cloudFoundry.put("cf_password", "password");
        cloudFoundry.put("cf_organization", "organization");
        systemPropertiesMap.add(cloudFoundry);
        HashMap<String, String> aws = new HashMap<>();
        aws.put("AWS_ACCESS_KEY_ID", "xyz");
        aws.put("AWS_SECRET_ACCESS_KEY", "abc");
        systemPropertiesMap.add(aws);
        systemPropertiesMap.forEach(properties -> properties.forEach(System::setProperty));
    }

    @Test
    public void cloudFoundryContextLoads () {
        ChaosEngine.main(new String[]{});
    }

    @After
    public void tearDown () {
        systemPropertiesMap.forEach(properties -> properties.keySet().forEach(System::clearProperty));
    }
}