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
        cloudFoundry.put("cf.apihost", "localhost");
        cloudFoundry.put("cf.username", "username");
        cloudFoundry.put("cf.password", "password");
        cloudFoundry.put("cf.organization", "organization");
        cloudFoundry.put("cf.space", "space!k");
        systemPropertiesMap.add(cloudFoundry);
        HashMap<String, String> awsEC2 = new HashMap<>();
        systemPropertiesMap.add(awsEC2);
        awsEC2.put("aws.ec2.accessKeyId", "accessKeyId");
        awsEC2.put("aws.ec2.secretAccessKey", "secretAccessKey");
        awsEC2.put("aws.ec2.region", "region");


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