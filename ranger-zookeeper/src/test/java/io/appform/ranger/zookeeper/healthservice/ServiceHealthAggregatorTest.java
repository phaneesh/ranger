/*
 * Copyright 2024 Authors, Flipkart Internet Pvt. Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.appform.ranger.zookeeper.healthservice;

import io.appform.ranger.core.healthcheck.HealthcheckStatus;
import io.appform.ranger.core.healthservice.ServiceHealthAggregator;
import io.appform.ranger.core.healthservice.TimeEntity;
import io.appform.ranger.core.healthservice.monitor.IsolatedHealthMonitor;
import io.appform.ranger.core.healthservice.monitor.Monitor;
import io.appform.ranger.core.utils.RangerTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

class ServiceHealthAggregatorTest {

    ServiceHealthAggregator serviceHealthAggregator = new ServiceHealthAggregator();
    TestMonitor testMonitor;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        testMonitor = new TestMonitor("TestHealthMonitor", TimeEntity.everySecond(), 1000);
        serviceHealthAggregator.addIsolatedMonitor(testMonitor);
        serviceHealthAggregator.addInlineMonitor(new Monitor<HealthcheckStatus>() {
            @Override
            public HealthcheckStatus monitor() {
                return HealthcheckStatus.healthy;
            }

            @Override
            public boolean isDisabled() {
                return false;
            }
        });

        serviceHealthAggregator.start();
        RangerTestUtils.sleepUntil(3, () -> serviceHealthAggregator.getRunning().get());
    }

    @AfterEach
    void tearDown() {
        serviceHealthAggregator.stop();
    }

    @Test
    void testStaleRun() {

        testMonitor.run();
        testMonitor.setThreadSleep(2000);

        RangerTestUtils.sleepUntil(3, () -> !testMonitor.hasValidUpdatedTime(new Date()));

        /* in the TestMonitor, thread was sleeping for 2 seconds, */
        /* so its state is supposed to be stale (>1 second) and service has to be unhealthy */
        Assertions.assertEquals(HealthcheckStatus.unhealthy, serviceHealthAggregator.getServiceHealth());

        testMonitor.setThreadSleep(5);
        RangerTestUtils.sleepUntil(3, () -> testMonitor.hasValidUpdatedTime(new Date()));

        /* in the TestMonitor, thread is sleeping only for 10 milliseconds, */
        /* so its state is supposed to be NOT stale (>1 second) and service has to be healthy */
        Assertions.assertEquals(HealthcheckStatus.healthy, serviceHealthAggregator.getServiceHealth());

    }

    @SuppressWarnings("rawtypes")
    private static class TestMonitor extends IsolatedHealthMonitor {
        int threadSleep = 2000;

        public TestMonitor(String name, TimeEntity timeEntity, long stalenessAllowedInMillis) {
            super(name, timeEntity, stalenessAllowedInMillis);
        }

        public void setThreadSleep(int threadSleep) {
            this.threadSleep = threadSleep;
        }

        @Override
        public synchronized HealthcheckStatus monitor() {
            RangerTestUtils.sleepFor(threadSleep);
            return HealthcheckStatus.healthy;
        }
    }
}