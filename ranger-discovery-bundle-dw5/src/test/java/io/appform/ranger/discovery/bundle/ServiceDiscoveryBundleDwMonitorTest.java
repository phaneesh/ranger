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

package io.appform.ranger.discovery.bundle;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.appform.ranger.core.healthcheck.HealthcheckStatus;
import io.appform.ranger.discovery.common.ServiceDiscoveryConfiguration;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.core.setup.AdminEnvironment;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.curator.test.TestingCluster;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static io.appform.ranger.discovery.bundle.TestUtils.assertNodeAbsence;
import static io.appform.ranger.discovery.bundle.TestUtils.assertNodePresence;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@Slf4j
class ServiceDiscoveryBundleDwMonitorTest {

    private final HealthCheckRegistry healthChecks = new HealthCheckRegistry();
    private final JerseyEnvironment jerseyEnvironment = mock(JerseyEnvironment.class);
    private final MetricRegistry metricRegistry = mock(MetricRegistry.class);
    private final LifecycleEnvironment lifecycleEnvironment = new LifecycleEnvironment(metricRegistry);
    private final Environment environment = mock(Environment.class);
    private final Bootstrap<?> bootstrap = mock(Bootstrap.class);
    private final Configuration configuration = mock(Configuration.class);
    private final DefaultServerFactory serverFactory = mock(DefaultServerFactory.class);
    private final ConnectorFactory connectorFactory = mock(HttpConnectorFactory.class);
    private final TestingCluster testingCluster = new TestingCluster(1);
    private final HealthcheckStatus status = HealthcheckStatus.healthy;
    private ServiceDiscoveryConfiguration serviceDiscoveryConfiguration;
    private final ServiceDiscoveryBundle<Configuration> bundle = new ServiceDiscoveryBundle<Configuration>() {
        @Override
        protected ServiceDiscoveryConfiguration getRangerConfiguration(Configuration configuration) {
            return serviceDiscoveryConfiguration;
        }

        @Override
        protected String getServiceName(Configuration configuration) {
            return "TestService";
        }

        @Override
        protected int getPort(Configuration configuration) {
            return 21000;
        }

        @Override
        protected String getHost() {
            return "CustomHost";
        }

    };

    @BeforeEach
    void setup() throws Exception {
        healthChecks.register("twice-healthy-only", new HealthCheck() {
            private final AtomicInteger counter = new AtomicInteger(5);

            @Override
            protected Result check() {
                val result = (counter.decrementAndGet() < 0)
                             ? Result.unhealthy("unhealthy")
                             : Result.healthy();
                log.info("Marking node as {}", result.isHealthy());
                return result;
            }
        });

        when(serverFactory.getApplicationConnectors()).thenReturn(Lists.newArrayList(connectorFactory));
        when(configuration.getServerFactory()).thenReturn(serverFactory);
        when(jerseyEnvironment.getResourceConfig()).thenReturn(new DropwizardResourceConfig());
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        when(environment.lifecycle()).thenReturn(lifecycleEnvironment);
        when(environment.healthChecks()).thenReturn(healthChecks);
        when(environment.getObjectMapper()).thenReturn(new ObjectMapper());
        AdminEnvironment adminEnvironment = mock(AdminEnvironment.class);
        doNothing().when(adminEnvironment)
                .addTask(any());
        when(environment.admin()).thenReturn(adminEnvironment);

        testingCluster.start();

        serviceDiscoveryConfiguration = ServiceDiscoveryConfiguration.builder()
                .zookeeper(testingCluster.getConnectString())
                .namespace("test")
                .environment("testing")
                .connectionRetryIntervalMillis(5000)
                .publishedHost("TestHost")
                .publishedPort(8021)
                .initialRotationStatus(true)
                .dropwizardCheckInterval(2)
                .dropwizardCheckStaleness(2)
                .build();
        bundle.initialize(bootstrap);
        bundle.run(configuration, environment);
        bundle.getServerStatus()
                .markStarted();
        for (LifeCycle lifeCycle : lifecycleEnvironment.getManagedObjects()) {
            lifeCycle.start();
        }
        bundle.registerHealthcheck(() -> status);
    }

    @AfterEach
    void tearDown() throws Exception {
        for (LifeCycle lifeCycle : lifecycleEnvironment.getManagedObjects()) {
            lifeCycle.stop();
        }
        testingCluster.stop();
    }

    @Test
    void testDiscovery() {
        assertNodePresence(bundle);
        val info = bundle.getServiceDiscoveryClient()
                .getNode()
                .orElse(null);
        Assertions.assertNotNull(info);
        Assertions.assertNotNull(info.getNodeData());
        Assertions.assertEquals("testing", info.getNodeData()
                .getEnvironment());
        Assertions.assertEquals("CustomHost", info.getHost());
        Assertions.assertEquals(21000, info.getPort());

        /* after 5 turns, the healthcheck will return unhealthy, and since dropwizardCheckInterval
           is 5 seconds, within 2*5=10 seconds, nodes should be absent */
        assertNodeAbsence(bundle);
    }
}