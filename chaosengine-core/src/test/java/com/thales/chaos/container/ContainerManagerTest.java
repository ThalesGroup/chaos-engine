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

package com.thales.chaos.container;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ContainerManagerTest {
    @Autowired
    private ContainerManager containerManager;
    @Spy
    private TestContainerClass testContainerClass;
    @Spy
    private TestContainerClass2 testContainerClass2;

    @Test
    public void getMatchingContainer () {
        String randomUUID = UUID.randomUUID().toString();
        String randomUUID2 = UUID.randomUUID().toString();
        containerManager.offer(testContainerClass);
        doReturn(true).when(testContainerClass).compareUniqueIdentifier(randomUUID);
        assertSame(testContainerClass, containerManager.getMatchingContainer(TestContainerClass.class, randomUUID));
        verify(testContainerClass, times(1)).compareUniqueIdentifier(randomUUID);
        verify(testContainerClass2, times(0)).compareUniqueIdentifier(randomUUID);
        reset(testContainerClass, testContainerClass2);
        // Two containers of different classes have the same identifier. Only return the class listed
        doReturn(false).when(testContainerClass).compareUniqueIdentifier(randomUUID2);
        doReturn(true).when(testContainerClass2).compareUniqueIdentifier(randomUUID2);
        assertNull(containerManager.getMatchingContainer(TestContainerClass.class, randomUUID2));
        verify(testContainerClass, times(1)).compareUniqueIdentifier(randomUUID2);
        verify(testContainerClass2, times(0)).compareUniqueIdentifier(randomUUID2);
    }

    @Test(expected = Test.None.class /* No exception expected */)
    @Repeat(10)
    public void threadSafe () {
        ExecutorService service = Executors.newFixedThreadPool(100);
        Stream.iterate(0, integer -> integer + 1).limit(10000).parallel().forEach(i -> service.execute(() -> containerManager.offer(Mockito.mock(Container.class))));
        containerManager.getMatchingContainer(Mockito.mock(Container.class).getClass(), "");
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        ContainerManager containerManager () {
            return new ContainerManager();
        }
    }

    abstract class TestContainerClass extends Container {
    }

    abstract class TestContainerClass2 extends Container {
    }
}