package com.gemalto.chaos;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ChaosEngineTest {
    @Test
    public void contextLoads () {
        ChaosEngine.main(new String[]{});
    }
}